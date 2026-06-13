package com.youfare.domain.auth;

import com.youfare.global.exception.BusinessException;
import com.youfare.global.jwt.JwtProvider;
import com.youfare.global.response.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 이메일 인증.
 * 6자리 코드를 생성해 서버 메모리에 보관(TTL)하고, 실제로 가입 이메일로 발송한다.
 *  - 발송은 Resend HTTP API로 한다. (Render가 SMTP 아웃바운드 포트를 차단해
 *    Gmail SMTP 연결이 무한정 매달리던 문제를 HTTP(443) 호출로 해결)
 *  - 검증 성공 시 JwtProvider로 단기 emailVerificationToken을 발급해
 *    회원가입·비밀번호 재설정 요청이 "이 이메일은 인증됐다"를 증명하도록 한다.
 *  - 저장소는 데모용 인메모리(서버 재시작 시 초기화). 다중 인스턴스 운영 시 Redis로 교체.
 */
@Slf4j
@Service
public class EmailVerificationService {

    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();
    // Resend 호출이 응답 없이 매달리지 않도록 응답 타임아웃을 둔다(과거 SMTP 무한 대기 재발 방지).
    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(10);

    private final JwtProvider jwtProvider;
    private final WebClient resendWebClient;
    private final String apiKey;
    private final String fromAddress;
    private final long codeTtlSeconds;

    public EmailVerificationService(JwtProvider jwtProvider,
                                    WebClient resendWebClient,
                                    @Value("${resend.api-key:}") String apiKey,
                                    @Value("${auth.email.from:onboarding@resend.dev}") String fromAddress,
                                    @Value("${auth.email.code-ttl-seconds:180}") long codeTtlSeconds) {
        this.jwtProvider = jwtProvider;
        this.resendWebClient = resendWebClient;
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
        this.codeTtlSeconds = codeTtlSeconds;
    }

    // 키 미설정 시 "인증요청만 누르면 항상 실패"의 원인을 기동 로그에서 바로 알 수 있게 한 번 경고한다.
    @PostConstruct
    void warnIfKeyMissing() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[이메일인증] RESEND_API_KEY 미설정 — /auth/email/send 가 항상 MAIL_SEND_FAILED 로 실패합니다. "
                    + "https://resend.com 에서 re_... 키를 발급해 환경변수에 넣으세요.");
        }
    }

    // email -> 발급된 코드 정보. 데모용 인메모리 저장(서버 재시작 시 초기화).
    private final ConcurrentHashMap<String, CodeEntry> store = new ConcurrentHashMap<>();

    private static class CodeEntry {
        final String code;
        final Instant expiresAt;
        int attempts;

        CodeEntry(String code, Instant expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }

    /** 인증번호 발급 + 이메일 발송. 발송 실패 시 저장 코드를 제거하고 예외를 던진다. */
    public int issueCode(String email) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        Duration ttl = Duration.ofSeconds(codeTtlSeconds);
        store.put(email, new CodeEntry(code, Instant.now().plus(ttl)));

        try {
            sendMail(email, code, ttl.toMinutes());
        } catch (RuntimeException e) {
            // 발송 실패 시 저장된 코드를 남겨두면 안 되므로 제거하고 예외를 그대로 전파한다.
            store.remove(email);
            throw e;
        }
        log.info("[이메일인증] 코드 발송 완료 email={} (TTL {}분)", email, ttl.toMinutes());
        return (int) codeTtlSeconds;
    }

    /**
     * 인증번호 검증. 성공하면 store에서 제거하고 emailVerificationToken을 발급한다.
     * 만료·시도초과·불일치는 각각 명확한 에러로 구분한다.
     */
    public String verifyCode(String email, String inputCode) {
        CodeEntry entry = store.get(email);
        if (entry == null) {
            throw new BusinessException(ErrorCode.EMAIL_CODE_NOT_FOUND);
        }
        if (Instant.now().isAfter(entry.expiresAt)) {
            store.remove(email);
            throw new BusinessException(ErrorCode.EMAIL_CODE_EXPIRED);
        }
        if (entry.attempts >= MAX_ATTEMPTS) {
            store.remove(email);
            throw new BusinessException(ErrorCode.EMAIL_CODE_TOO_MANY_ATTEMPTS);
        }
        if (!entry.code.equals(inputCode)) {
            entry.attempts++;
            throw new BusinessException(ErrorCode.EMAIL_CODE_MISMATCH);
        }

        store.remove(email);
        return jwtProvider.generateEmailToken(email);
    }

    /** Resend HTTP API로 인증번호 메일을 발송한다. 실패 시 MAIL_SEND_FAILED. */
    private void sendMail(String to, String code, long ttlMinutes) {
        Map<String, Object> body = Map.of(
                "from", fromAddress,
                "to", new String[]{to},
                "subject", "[YouFare] 이메일 인증번호",
                "text",
                "YouFare 이메일 인증번호입니다.\n\n"
                        + "인증번호: " + code + "\n\n"
                        + "유효시간은 " + ttlMinutes + "분입니다.\n"
                        + "본인이 요청하지 않았다면 이 메일을 무시하세요."
        );

        try {
            resendWebClient.post()
                    .uri("/emails")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(SEND_TIMEOUT)
                    .block();
        } catch (WebClientResponseException e) {
            // Resend가 4xx/5xx로 거절(키 오류·도메인 미인증 등). 본문에 사유가 들어있어 함께 남긴다.
            log.error("인증 메일 발송 실패(Resend): email={}, status={}, body={}",
                    to, e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED);
        } catch (Exception e) {
            // 타임아웃·네트워크 오류 등.
            log.error("인증 메일 발송 실패(Resend): email={}, error={}", to, e.getMessage());
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED);
        }
    }
}
