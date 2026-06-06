package com.youfare.domain.auth;

import com.youfare.domain.user.Provider;
import com.youfare.domain.user.User;
import com.youfare.domain.user.UserRepository;
import com.youfare.global.exception.BusinessException;
import com.youfare.global.jwt.JwtProvider;
import com.youfare.global.response.ErrorCode;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 폼 회원가입/로그인 핵심 로직 검증.
 * 실제 BCrypt 인코더를 써서 해싱·매칭까지 검증하고, repository/JWT는 목으로 격리한다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock JwtProvider jwtProvider;
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    AuthService authService;

    private static final String PHONE = "01012345678";
    private static final String PHONE_TOKEN = "phone-token";

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtProvider);
    }

    private SignupRequest signupRequest(String email, String password, String nickname,
                                        String phone, String phoneToken,
                                        boolean agreeTerms, boolean agreePrivacy) {
        SignupRequest req = new SignupRequest();
        ReflectionTestUtils.setField(req, "email", email);
        ReflectionTestUtils.setField(req, "password", password);
        ReflectionTestUtils.setField(req, "nickname", nickname);
        ReflectionTestUtils.setField(req, "phoneNumber", phone);
        ReflectionTestUtils.setField(req, "phoneVerificationToken", phoneToken);
        ReflectionTestUtils.setField(req, "agreeTerms", agreeTerms);
        ReflectionTestUtils.setField(req, "agreePrivacy", agreePrivacy);
        return req;
    }

    /** 약관 동의 + 전화 인증이 모두 통과되는 정상 요청 */
    private SignupRequest validSignup(String email) {
        return signupRequest(email, "password123", "닉", PHONE, PHONE_TOKEN, true, true);
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest req = new LoginRequest();
        ReflectionTestUtils.setField(req, "email", email);
        ReflectionTestUtils.setField(req, "password", password);
        return req;
    }

    @Test
    @DisplayName("회원가입 성공 — 이메일 정규화, 비밀번호 해싱, 전화번호 저장, 토큰 발급")
    void signup_success() {
        given(jwtProvider.getVerifiedPhone(PHONE_TOKEN)).willReturn(PHONE);
        given(userRepository.existsByEmailAndProvider("user@test.com", Provider.LOCAL)).willReturn(false);
        given(userRepository.existsByPhoneNumberAndProvider(PHONE, Provider.LOCAL)).willReturn(false);
        given(userRepository.saveAndFlush(any(User.class))).willAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 1L);
            return u;
        });
        given(jwtProvider.generateToken(1L)).willReturn("jwt-token");

        SignupRequest req = signupRequest("  USER@Test.com ", "password123", " 길동 ", PHONE, PHONE_TOKEN, true, true);
        TokenResponse res = authService.signup(req);

        assertThat(res.getUserId()).isEqualTo(1L);
        assertThat(res.getAccessToken()).isEqualTo("jwt-token");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("user@test.com");
        assertThat(saved.getProvider()).isEqualTo(Provider.LOCAL);
        assertThat(saved.getPhoneNumber()).isEqualTo(PHONE);
        assertThat(saved.getNickname()).isEqualTo("길동");
        assertThat(saved.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("회원가입 실패 — 필수 약관 미동의")
    void signup_termsNotAgreed() {
        SignupRequest req = signupRequest("user@test.com", "password123", "닉", PHONE, PHONE_TOKEN, true, false);

        assertThatThrownBy(() -> authService.signup(req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TERMS_NOT_AGREED);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("회원가입 실패 — 전화 인증 토큰이 유효하지 않음")
    void signup_phoneTokenInvalid() {
        given(jwtProvider.getVerifiedPhone(PHONE_TOKEN)).willThrow(new JwtException("bad"));

        assertThatThrownBy(() -> authService.signup(validSignup("user@test.com")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PHONE_NOT_VERIFIED);
    }

    @Test
    @DisplayName("회원가입 실패 — 토큰의 번호와 가입 번호가 불일치")
    void signup_phoneMismatch() {
        given(jwtProvider.getVerifiedPhone(PHONE_TOKEN)).willReturn("01099999999");

        assertThatThrownBy(() -> authService.signup(validSignup("user@test.com")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PHONE_NOT_VERIFIED);
    }

    @Test
    @DisplayName("회원가입 실패 — 이미 가입된 이메일")
    void signup_duplicateEmail() {
        given(jwtProvider.getVerifiedPhone(PHONE_TOKEN)).willReturn(PHONE);
        given(userRepository.existsByEmailAndProvider("dup@test.com", Provider.LOCAL)).willReturn(true);

        assertThatThrownBy(() -> authService.signup(validSignup("dup@test.com")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("회원가입 실패 — 이미 가입된 전화번호")
    void signup_duplicatePhone() {
        given(jwtProvider.getVerifiedPhone(PHONE_TOKEN)).willReturn(PHONE);
        given(userRepository.existsByEmailAndProvider(anyString(), eq(Provider.LOCAL))).willReturn(false);
        given(userRepository.existsByPhoneNumberAndProvider(PHONE, Provider.LOCAL)).willReturn(true);

        assertThatThrownBy(() -> authService.signup(validSignup("user@test.com")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PHONE_ALREADY_EXISTS);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("회원가입 실패 — 동시 가입 race로 검사를 통과해도 DB 유니크 제약이 막는다")
    void signup_raceConditionHandledAsDuplicate() {
        given(jwtProvider.getVerifiedPhone(PHONE_TOKEN)).willReturn(PHONE);
        given(userRepository.existsByEmailAndProvider(anyString(), eq(Provider.LOCAL))).willReturn(false);
        given(userRepository.existsByPhoneNumberAndProvider(anyString(), eq(Provider.LOCAL))).willReturn(false);
        given(userRepository.saveAndFlush(any(User.class)))
                .willThrow(new DataIntegrityViolationException("unique violation"));

        assertThatThrownBy(() -> authService.signup(validSignup("race@test.com")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("로그인 성공 — 비밀번호 매칭 후 토큰 발급")
    void login_success() {
        User user = User.ofLocal("user@test.com", passwordEncoder.encode("password123"), "닉", PHONE);
        ReflectionTestUtils.setField(user, "id", 7L);
        given(userRepository.findByEmailAndProvider("user@test.com", Provider.LOCAL)).willReturn(Optional.of(user));
        given(jwtProvider.generateToken(7L)).willReturn("jwt-7");

        TokenResponse res = authService.login(loginRequest("USER@test.com", "password123"));

        assertThat(res.getUserId()).isEqualTo(7L);
        assertThat(res.getAccessToken()).isEqualTo("jwt-7");
    }

    @Test
    @DisplayName("로그인 실패 — 비밀번호 불일치 (LOGIN_FAILED)")
    void login_wrongPassword() {
        User user = User.ofLocal("user@test.com", passwordEncoder.encode("password123"), "닉", PHONE);
        given(userRepository.findByEmailAndProvider("user@test.com", Provider.LOCAL)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(loginRequest("user@test.com", "wrongpass")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LOGIN_FAILED);

        verify(jwtProvider, never()).generateToken(anyLong());
    }

    @Test
    @DisplayName("로그인 실패 — 존재하지 않는 이메일도 동일한 LOGIN_FAILED로 응답(계정 존재 노출 방지)")
    void login_unknownEmail() {
        given(userRepository.findByEmailAndProvider("none@test.com", Provider.LOCAL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest("none@test.com", "password123")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LOGIN_FAILED);
    }

    private FindEmailRequest findEmailRequest(String phoneToken) {
        FindEmailRequest req = new FindEmailRequest();
        ReflectionTestUtils.setField(req, "phoneVerificationToken", phoneToken);
        return req;
    }

    private ResetPasswordRequest resetRequest(String email, String phoneToken, String newPassword) {
        ResetPasswordRequest req = new ResetPasswordRequest();
        ReflectionTestUtils.setField(req, "email", email);
        ReflectionTestUtils.setField(req, "phoneVerificationToken", phoneToken);
        ReflectionTestUtils.setField(req, "newPassword", newPassword);
        return req;
    }

    @Test
    @DisplayName("아이디 찾기 성공 — 전화 인증 번호로 LOCAL 계정의 가입 이메일 전체를 반환")
    void findEmail_success() {
        User user = User.ofLocal("user@test.com", passwordEncoder.encode("password123"), "닉", PHONE);
        given(jwtProvider.getVerifiedPhone(PHONE_TOKEN)).willReturn(PHONE);
        given(userRepository.findByPhoneNumberAndProvider(PHONE, Provider.LOCAL)).willReturn(Optional.of(user));

        FindEmailResponse res = authService.findEmail(findEmailRequest(PHONE_TOKEN));

        assertThat(res.getEmail()).isEqualTo("user@test.com"); // 가리지 않은 전체 이메일
    }

    @Test
    @DisplayName("아이디 찾기 실패 — 해당 번호로 가입한 LOCAL 계정 없음(ACCOUNT_NOT_FOUND)")
    void findEmail_notFound() {
        given(jwtProvider.getVerifiedPhone(PHONE_TOKEN)).willReturn(PHONE);
        given(userRepository.findByPhoneNumberAndProvider(PHONE, Provider.LOCAL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.findEmail(findEmailRequest(PHONE_TOKEN)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("비밀번호 재설정 성공 — 전화 인증 번호로 계정을 찾아 새 비밀번호로 교체")
    void resetPassword_success() {
        User user = User.ofLocal("user@test.com", passwordEncoder.encode("oldpassword"), "닉", PHONE);
        given(jwtProvider.getVerifiedPhone(PHONE_TOKEN)).willReturn(PHONE);
        given(userRepository.findByPhoneNumberAndProvider(PHONE, Provider.LOCAL)).willReturn(Optional.of(user));

        authService.resetPassword(resetRequest("user@test.com", PHONE_TOKEN, "newpassword123"));

        assertThat(passwordEncoder.matches("newpassword123", user.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("oldpassword", user.getPassword())).isFalse();
    }

    @Test
    @DisplayName("비밀번호 재설정 성공 — 입력 이메일이 가입 이메일과 달라도 전화 인증 번호만 맞으면 교체(오타로 막히지 않음)")
    void resetPassword_emailMismatchStillSucceeds() {
        User user = User.ofLocal("user@test.com", passwordEncoder.encode("oldpassword"), "닉", PHONE);
        given(jwtProvider.getVerifiedPhone(PHONE_TOKEN)).willReturn(PHONE);
        given(userRepository.findByPhoneNumberAndProvider(PHONE, Provider.LOCAL)).willReturn(Optional.of(user));

        // 사용자가 이메일을 잘못 입력해도(USER@Test.com / typo) 전화 인증이 본인을 증명하므로 성공해야 한다.
        authService.resetPassword(resetRequest("WRONG@typo.com", PHONE_TOKEN, "newpassword123"));

        assertThat(passwordEncoder.matches("newpassword123", user.getPassword())).isTrue();
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 — 해당 번호로 가입한 LOCAL 계정 없음(ACCOUNT_NOT_FOUND)")
    void resetPassword_phoneNotRegistered() {
        given(jwtProvider.getVerifiedPhone(PHONE_TOKEN)).willReturn(PHONE);
        given(userRepository.findByPhoneNumberAndProvider(PHONE, Provider.LOCAL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword(resetRequest("user@test.com", PHONE_TOKEN, "newpassword123")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 — 전화 인증 토큰이 유효하지 않음(PHONE_NOT_VERIFIED)")
    void resetPassword_invalidPhoneToken() {
        given(jwtProvider.getVerifiedPhone(PHONE_TOKEN)).willThrow(new JwtException("bad"));

        assertThatThrownBy(() -> authService.resetPassword(resetRequest("user@test.com", PHONE_TOKEN, "newpassword123")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PHONE_NOT_VERIFIED);

        verify(userRepository, never()).findByPhoneNumberAndProvider(anyString(), any());
    }
}
