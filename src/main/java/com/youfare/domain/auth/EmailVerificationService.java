package com.youfare.domain.auth;

import com.youfare.global.exception.BusinessException;
import com.youfare.global.jwt.JwtProvider;
import com.youfare.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 이메일 인증.
 * 6자리 코드를 생성해 서버 메모리에 보관(TTL)하고, 실제로 가입 이메일로 발송한다.
 *  - 검증 성공 시 JwtProvider로 단기 emailVerificationToken을 발급해
 *    회원가입·비밀번호 재설정 요청이 "이 이메일은 인증됐다"를 증명하도록 한다.
 *  - 저장소는 데모용 인메모리(서버 재시작 시 초기화). 다중 인스턴스 운영 시 Redis로 교체.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtProvider jwtProvider;
    private final JavaMailSender mailSender;

    @Value("${auth.email.code-ttl-seconds:180}")
    private long codeTtlSeconds;

    @Value("${spring.mail.username:}")
    private String fromAddress;

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
        } catch (MailException e) {
            store.remove(email);
            log.error("인증 메일 발송 실패: email={}, error={}", email, e.getMessage());
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED);
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

    private void sendMail(String to, String code, long ttlMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (fromAddress != null && !fromAddress.isBlank()) {
            message.setFrom(fromAddress);
        }
        message.setTo(to);
        message.setSubject("[YouFare] 이메일 인증번호");
        message.setText(
                "YouFare 이메일 인증번호입니다.\n\n"
                        + "인증번호: " + code + "\n\n"
                        + "유효시간은 " + ttlMinutes + "분입니다.\n"
                        + "본인이 요청하지 않았다면 이 메일을 무시하세요."
        );
        mailSender.send(message);
    }
}
