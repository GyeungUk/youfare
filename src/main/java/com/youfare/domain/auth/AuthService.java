package com.youfare.domain.auth;

import com.youfare.domain.user.Provider;
import com.youfare.domain.user.User;
import com.youfare.domain.user.UserRepository;
import com.youfare.global.exception.BusinessException;
import com.youfare.global.jwt.JwtProvider;
import com.youfare.global.response.ErrorCode;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /** 폼 회원가입 — 약관 동의·전화 인증 검증 후 BCrypt 해시로 저장, 즉시 accessToken 발급 */
    @Transactional
    public TokenResponse signup(SignupRequest request) {
        // 0. 필수 약관 동의 (DTO @AssertTrue로 1차 검증되지만, 서비스에서도 방어)
        if (!request.isAgreeTerms() || !request.isAgreePrivacy()) {
            throw new BusinessException(ErrorCode.TERMS_NOT_AGREED);
        }

        String email = normalizeEmail(request.getEmail());
        String phone = normalizePhone(request.getPhoneNumber());

        // 1. 전화 인증 토큰 검증 — 토큰이 가리키는 번호와 입력 번호가 일치해야 함
        verifyPhoneToken(request.getPhoneVerificationToken(), phone);

        // 2. 애플리케이션 레벨 중복 검사 (친절한 메시지)
        if (userRepository.existsByEmailAndProvider(email, Provider.LOCAL)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.existsByPhoneNumberAndProvider(phone, Provider.LOCAL)) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        User user = User.ofLocal(
                email,
                passwordEncoder.encode(request.getPassword()),
                request.getNickname().trim(),
                phone
        );

        try {
            // 3. 동시 가입 race로 검사를 통과해도 DB 유니크 제약이 막는다.
            //    saveAndFlush로 즉시 INSERT를 발생시켜 여기서 예외를 잡는다(커밋 시점 누락 방지).
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        return TokenResponse.of(user.getId(), jwtProvider.generateToken(user.getId()));
    }

    /**
     * 아이디(이메일) 찾기 — 전화 인증 토큰의 번호로 가입한 LOCAL 계정을 찾아 가입 이메일 전체를 반환.
     * 소셜 가입자는 비밀번호/폼 로그인이 없어 대상에서 제외(LOCAL만 조회).
     */
    @Transactional(readOnly = true)
    public FindEmailResponse findEmail(FindEmailRequest request) {
        String phone = verifiedPhoneFromToken(request.getPhoneVerificationToken());

        User user = userRepository.findByPhoneNumberAndProvider(phone, Provider.LOCAL)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        return FindEmailResponse.builder()
                .email(user.getEmail())
                .build();
    }

    /**
     * 비밀번호 재설정 — 전화 인증을 마친 번호로 가입한 LOCAL 계정을 찾아 새 비밀번호로 교체.
     * 본인 확인은 전화 OTP가 책임지며(전화번호는 LOCAL 내에서 유일), 입력 이메일은 화면 표시용일 뿐
     * 매칭 조건이 아니다. (이메일 대소문자·오타로 "계정을 찾을 수 없음"이 뜨던 문제를 없앤다.)
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String phone = verifiedPhoneFromToken(request.getPhoneVerificationToken());

        User user = userRepository.findByPhoneNumberAndProvider(phone, Provider.LOCAL)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    /** 전화 인증 토큰을 검증하고 정규화된 번호를 반환 (실패 시 PHONE_NOT_VERIFIED) */
    private String verifiedPhoneFromToken(String phoneToken) {
        try {
            return normalizePhone(jwtProvider.getVerifiedPhone(phoneToken));
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.PHONE_NOT_VERIFIED);
        }
    }

    /** phoneVerificationToken이 유효하고, 그 안의 번호가 가입 번호와 같은지 확인 */
    private void verifyPhoneToken(String phoneToken, String phone) {
        String verifiedPhone;
        try {
            verifiedPhone = jwtProvider.getVerifiedPhone(phoneToken);
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.PHONE_NOT_VERIFIED);
        }
        if (!phone.equals(normalizePhone(verifiedPhone))) {
            throw new BusinessException(ErrorCode.PHONE_NOT_VERIFIED);
        }
    }

    /** 폼 로그인 — 이메일/비밀번호 검증. 이메일 미존재·비밀번호 불일치 모두 동일 메시지로 응답해 계정 존재 여부 노출 방지 */
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndProvider(normalizeEmail(request.getEmail()), Provider.LOCAL)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        return TokenResponse.of(user.getId(), jwtProvider.generateToken(user.getId()));
    }

    /** 대소문자·공백 차이로 같은 메일이 다른 계정이 되는 것을 막기 위해 정규화 */
    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /** 하이픈·공백 등을 제거해 숫자만 남긴다 (010-1234-5678 → 01012345678) */
    private String normalizePhone(String phone) {
        return phone == null ? null : phone.replaceAll("[^0-9]", "");
    }
}
