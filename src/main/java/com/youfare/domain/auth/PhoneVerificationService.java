package com.youfare.domain.auth;

import com.youfare.global.exception.BusinessException;
import com.youfare.global.jwt.JwtProvider;
import com.youfare.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 전화번호 인증 (목업).
 * 실제 SMS를 보내지 않고 6자리 코드를 서버 메모리에 보관한다.
 *  - 데모 편의를 위해 send 응답과 서버 로그로 코드를 노출한다(운영에선 절대 노출 금지).
 *  - 검증 성공 시 JwtProvider로 단기 phoneVerificationToken을 발급해
 *    회원가입 요청이 "이 번호는 인증됐다"를 증명하도록 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private static final Duration CODE_TTL = Duration.ofMinutes(3);
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtProvider jwtProvider;

    // phoneNumber -> 발급된 코드 정보. 데모용 인메모리 저장(서버 재시작 시 초기화).
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

    /** 인증번호 발급. 데모이므로 생성된 코드를 그대로 반환한다(프론트가 화면에 노출). */
    public String issueCode(String phoneNumber) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        store.put(phoneNumber, new CodeEntry(code, Instant.now().plus(CODE_TTL)));
        log.info("[전화인증-목업] phone={} code={} (TTL {}분)", phoneNumber, code, CODE_TTL.toMinutes());
        return code;
    }

    /**
     * 인증번호 검증. 성공하면 store에서 제거하고 phoneVerificationToken을 발급한다.
     * 만료·시도초과·불일치는 각각 명확한 에러로 구분한다.
     */
    public String verifyCode(String phoneNumber, String inputCode) {
        CodeEntry entry = store.get(phoneNumber);
        if (entry == null) {
            throw new BusinessException(ErrorCode.PHONE_CODE_NOT_FOUND);
        }
        if (Instant.now().isAfter(entry.expiresAt)) {
            store.remove(phoneNumber);
            throw new BusinessException(ErrorCode.PHONE_CODE_EXPIRED);
        }
        if (entry.attempts >= MAX_ATTEMPTS) {
            store.remove(phoneNumber);
            throw new BusinessException(ErrorCode.PHONE_CODE_TOO_MANY_ATTEMPTS);
        }
        if (!entry.code.equals(inputCode)) {
            entry.attempts++;
            throw new BusinessException(ErrorCode.PHONE_CODE_MISMATCH);
        }

        store.remove(phoneNumber);
        return jwtProvider.generatePhoneToken(phoneNumber);
    }
}
