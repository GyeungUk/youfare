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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /** 폼 회원가입 — 약관 동의·이메일 인증 검증 후 BCrypt 해시로 저장, 즉시 accessToken 발급 */
    @Transactional
    public TokenResponse signup(SignupRequest request) {
        // 0. 필수 약관 동의 (DTO @AssertTrue로 1차 검증되지만, 서비스에서도 방어)
        if (!request.isAgreeTerms() || !request.isAgreePrivacy()) {
            throw new BusinessException(ErrorCode.TERMS_NOT_AGREED);
        }

        String email = normalizeEmail(request.getEmail());
        String username = normalizeUsername(request.getUsername());

        // 1. 이메일 인증 토큰 검증 — 토큰이 가리키는 이메일과 가입 이메일이 일치해야 함
        verifyEmailToken(request.getEmailVerificationToken(), email);

        // 2. 애플리케이션 레벨 중복 검사 (친절한 메시지)
        if (userRepository.existsByEmailAndProvider(email, Provider.LOCAL)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        // 같은 이메일이 이미 소셜(네이버/카카오)로 가입돼 있으면 폼 가입을 막고 그 소셜로 로그인하도록 안내.
        // (이게 없으면 같은 이메일이 LOCAL/소셜 두 계정으로 쪼개져 "내 계정이 둘"이 되는 혼란을 부른다)
        socialAccountConflict(email).ifPresent(e -> { throw e; });
        if (userRepository.existsByUsernameAndProvider(username, Provider.LOCAL)) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        User user = User.ofLocal(
                email,
                username,
                passwordEncoder.encode(request.getPassword()),
                request.getNickname().trim()
        );

        try {
            // 3. 동시 가입 race로 검사를 통과해도 DB 유니크 제약이 막는다.
            //    saveAndFlush로 즉시 INSERT를 발생시켜 여기서 예외를 잡는다(커밋 시점 누락 방지).
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // 이메일·아이디 어느 쪽 제약 위반인지 구분이 어려워 둘 다 안내 가능한 메시지로 통일.
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        return TokenResponse.of(user.getId(), jwtProvider.generateToken(user.getId()));
    }

    /**
     * 아이디 찾기 — 이메일 인증을 마친 토큰으로 그 이메일로 가입한 LOCAL 계정의 아이디를 돌려준다.
     * 본인 확인은 이메일 OTP가 책임진다(인증 안 된 이메일로는 조회 불가).
     */
    @Transactional(readOnly = true)
    public String findUsername(String emailVerificationToken) {
        String email = verifiedEmailFromToken(emailVerificationToken);

        User user = findUsableLocalAccount(email)
                .orElseThrow(() -> accountNotFoundOrSocial(email));

        return user.getUsername();
    }

    /**
     * 비밀번호 재설정 가능 여부 사전 확인 — 새 비밀번호 입력 전에 호출한다.
     * 재설정 가능한 LOCAL 계정이면 정상 반환, 소셜 계정/미존재면 안내 예외를 던져
     * 사용자가 비밀번호를 헛입력하기 전에 알려준다. (검증 로직은 resetPassword와 동일)
     */
    @Transactional(readOnly = true)
    public void checkResettable(String emailVerificationToken) {
        String email = verifiedEmailFromToken(emailVerificationToken);
        findUsableLocalAccount(email)
                .orElseThrow(() -> accountNotFoundOrSocial(email));
    }

    /**
     * 비밀번호 재설정 — 이메일 인증을 마친 이메일로 가입한 LOCAL 계정을 찾아 새 비밀번호로 교체.
     * 본인 확인은 이메일 OTP가 책임진다.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = verifiedEmailFromToken(request.getEmailVerificationToken());

        User user = findUsableLocalAccount(email)
                .orElseThrow(() -> accountNotFoundOrSocial(email));

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    /**
     * 폼 로그인이 실제로 가능한 LOCAL 계정만 찾는다.
     * username이 비어 있는 LOCAL 행은 로그인 식별자가 없어 로그인 불가한 잔재 데이터(과거 구현 흔적)이므로
     * "없는 것"으로 취급해, 아이디찾기/비번재설정이 소셜 계정 안내로 폴백하도록 한다.
     */
    private Optional<User> findUsableLocalAccount(String email) {
        return userRepository.findByEmailAndProvider(email, Provider.LOCAL)
                .filter(u -> u.getUsername() != null && !u.getUsername().isBlank());
    }

    /**
     * 같은 이메일이 소셜(비-LOCAL)로 가입돼 있으면, 그 provider로 로그인하라는 예외를 Optional로 반환.
     * 소셜 계정이 없으면 empty. 폼 가입 차단·아이디찾기·비번재설정 안내에 공통으로 쓴다.
     */
    private Optional<BusinessException> socialAccountConflict(String email) {
        return userRepository.findFirstByEmailIgnoreCaseAndProviderNot(email, Provider.LOCAL)
                .map(social -> {
                    String name = social.getProvider().getDisplayName();
                    return new BusinessException(ErrorCode.SOCIAL_ACCOUNT_EXISTS,
                            name + " 로그인으로 가입된 이메일이에요. " + name + "로 로그인해 주세요.");
                });
    }

    /** LOCAL 계정을 못 찾았을 때: 소셜 계정이 있으면 그 안내를, 없으면 ACCOUNT_NOT_FOUND를 돌려준다. */
    private BusinessException accountNotFoundOrSocial(String email) {
        return socialAccountConflict(email)
                .orElseGet(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    /** 이메일 인증 토큰을 검증하고 정규화된 이메일을 반환 (실패 시 EMAIL_NOT_VERIFIED) */
    private String verifiedEmailFromToken(String emailToken) {
        try {
            return normalizeEmail(jwtProvider.getVerifiedEmail(emailToken));
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    /** emailVerificationToken이 유효하고, 그 안의 이메일이 가입 이메일과 같은지 확인 */
    private void verifyEmailToken(String emailToken, String email) {
        String verifiedEmail;
        try {
            verifiedEmail = jwtProvider.getVerifiedEmail(emailToken);
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        if (!email.equals(normalizeEmail(verifiedEmail))) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    /** 폼 로그인 — 아이디/비밀번호 검증. 아이디 미존재·비밀번호 불일치 모두 동일 메시지로 응답해 계정 존재 여부 노출 방지 */
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameAndProvider(normalizeUsername(request.getUsername()), Provider.LOCAL)
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

    /** 아이디도 대소문자·공백 차이로 중복/로그인 실패가 나지 않도록 동일하게 정규화 */
    private String normalizeUsername(String username) {
        return username == null ? null : username.trim().toLowerCase(Locale.ROOT);
    }
}
