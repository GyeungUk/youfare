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

    private static final String EMAIL = "user@test.com";
    private static final String EMAIL_TOKEN = "email-token";

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtProvider);
    }

    private SignupRequest signupRequest(String email, String username, String password, String nickname,
                                        String emailToken, boolean agreeTerms, boolean agreePrivacy) {
        SignupRequest req = new SignupRequest();
        ReflectionTestUtils.setField(req, "email", email);
        ReflectionTestUtils.setField(req, "username", username);
        ReflectionTestUtils.setField(req, "password", password);
        ReflectionTestUtils.setField(req, "nickname", nickname);
        ReflectionTestUtils.setField(req, "emailVerificationToken", emailToken);
        ReflectionTestUtils.setField(req, "agreeTerms", agreeTerms);
        ReflectionTestUtils.setField(req, "agreePrivacy", agreePrivacy);
        return req;
    }

    /** 약관 동의 + 이메일 인증이 모두 통과되는 정상 요청 */
    private SignupRequest validSignup(String email) {
        return signupRequest(email, "tester1", "password123", "닉", EMAIL_TOKEN, true, true);
    }

    private LoginRequest loginRequest(String username, String password) {
        LoginRequest req = new LoginRequest();
        ReflectionTestUtils.setField(req, "username", username);
        ReflectionTestUtils.setField(req, "password", password);
        return req;
    }

    @Test
    @DisplayName("회원가입 성공 — 이메일 정규화, 비밀번호 해싱, 토큰 발급")
    void signup_success() {
        given(jwtProvider.getVerifiedEmail(EMAIL_TOKEN)).willReturn(EMAIL);
        given(userRepository.existsByEmailAndProvider(EMAIL, Provider.LOCAL)).willReturn(false);
        given(userRepository.saveAndFlush(any(User.class))).willAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 1L);
            return u;
        });
        given(jwtProvider.generateToken(1L)).willReturn("jwt-token");

        // 입력 이메일/아이디는 대소문자·공백이 섞여도 정규화돼 저장돼야 한다.
        SignupRequest req = signupRequest("  USER@Test.com ", "  Tester_01 ", "password123", " 길동 ", EMAIL_TOKEN, true, true);
        TokenResponse res = authService.signup(req);

        assertThat(res.getUserId()).isEqualTo(1L);
        assertThat(res.getAccessToken()).isEqualTo("jwt-token");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getUsername()).isEqualTo("tester_01");
        assertThat(saved.getProvider()).isEqualTo(Provider.LOCAL);
        assertThat(saved.getNickname()).isEqualTo("길동");
        assertThat(saved.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("회원가입 실패 — 필수 약관 미동의")
    void signup_termsNotAgreed() {
        SignupRequest req = signupRequest(EMAIL, "tester1", "password123", "닉", EMAIL_TOKEN, true, false);

        assertThatThrownBy(() -> authService.signup(req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TERMS_NOT_AGREED);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("회원가입 실패 — 이메일 인증 토큰이 유효하지 않음")
    void signup_emailTokenInvalid() {
        given(jwtProvider.getVerifiedEmail(EMAIL_TOKEN)).willThrow(new JwtException("bad"));

        assertThatThrownBy(() -> authService.signup(validSignup(EMAIL)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);
    }

    @Test
    @DisplayName("회원가입 실패 — 토큰의 이메일과 가입 이메일이 불일치")
    void signup_emailMismatch() {
        given(jwtProvider.getVerifiedEmail(EMAIL_TOKEN)).willReturn("other@test.com");

        assertThatThrownBy(() -> authService.signup(validSignup(EMAIL)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);
    }

    @Test
    @DisplayName("회원가입 실패 — 이미 가입된 이메일")
    void signup_duplicateEmail() {
        given(jwtProvider.getVerifiedEmail(EMAIL_TOKEN)).willReturn("dup@test.com");
        given(userRepository.existsByEmailAndProvider("dup@test.com", Provider.LOCAL)).willReturn(true);

        assertThatThrownBy(() -> authService.signup(validSignup("dup@test.com")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("회원가입 실패 — 이미 사용 중인 아이디")
    void signup_duplicateUsername() {
        given(jwtProvider.getVerifiedEmail(EMAIL_TOKEN)).willReturn(EMAIL);
        given(userRepository.existsByEmailAndProvider(EMAIL, Provider.LOCAL)).willReturn(false);
        given(userRepository.existsByUsernameAndProvider("tester1", Provider.LOCAL)).willReturn(true);

        assertThatThrownBy(() -> authService.signup(validSignup(EMAIL)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USERNAME_ALREADY_EXISTS);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("회원가입 실패 — 동시 가입 race로 검사를 통과해도 DB 유니크 제약이 막는다")
    void signup_raceConditionHandledAsDuplicate() {
        given(jwtProvider.getVerifiedEmail(EMAIL_TOKEN)).willReturn("race@test.com");
        given(userRepository.existsByEmailAndProvider(anyString(), eq(Provider.LOCAL))).willReturn(false);
        given(userRepository.saveAndFlush(any(User.class)))
                .willThrow(new DataIntegrityViolationException("unique violation"));

        assertThatThrownBy(() -> authService.signup(validSignup("race@test.com")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("로그인 성공 — 아이디 정규화 후 비밀번호 매칭, 토큰 발급")
    void login_success() {
        User user = User.ofLocal(EMAIL, "tester1", passwordEncoder.encode("password123"), "닉");
        ReflectionTestUtils.setField(user, "id", 7L);
        given(userRepository.findByUsernameAndProvider("tester1", Provider.LOCAL)).willReturn(Optional.of(user));
        given(jwtProvider.generateToken(7L)).willReturn("jwt-7");

        // 입력 아이디는 대소문자/공백이 섞여도 정규화 후 조회돼야 한다.
        TokenResponse res = authService.login(loginRequest(" Tester1 ", "password123"));

        assertThat(res.getUserId()).isEqualTo(7L);
        assertThat(res.getAccessToken()).isEqualTo("jwt-7");
    }

    @Test
    @DisplayName("로그인 실패 — 비밀번호 불일치 (LOGIN_FAILED)")
    void login_wrongPassword() {
        User user = User.ofLocal(EMAIL, "tester1", passwordEncoder.encode("password123"), "닉");
        given(userRepository.findByUsernameAndProvider("tester1", Provider.LOCAL)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(loginRequest("tester1", "wrongpass")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LOGIN_FAILED);

        verify(jwtProvider, never()).generateToken(anyLong());
    }

    @Test
    @DisplayName("로그인 실패 — 존재하지 않는 아이디도 동일한 LOGIN_FAILED로 응답(계정 존재 노출 방지)")
    void login_unknownUsername() {
        given(userRepository.findByUsernameAndProvider("none", Provider.LOCAL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest("none", "password123")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LOGIN_FAILED);
    }

    @Test
    @DisplayName("아이디 찾기 성공 — 인증된 이메일로 가입한 계정의 아이디 반환")
    void findUsername_success() {
        User user = User.ofLocal(EMAIL, "tester1", passwordEncoder.encode("password123"), "닉");
        given(jwtProvider.getVerifiedEmail(EMAIL_TOKEN)).willReturn(EMAIL);
        given(userRepository.findByEmailAndProvider(EMAIL, Provider.LOCAL)).willReturn(Optional.of(user));

        assertThat(authService.findUsername(EMAIL_TOKEN)).isEqualTo("tester1");
    }

    @Test
    @DisplayName("아이디 찾기 실패 — 해당 이메일로 가입한 LOCAL 계정 없음(ACCOUNT_NOT_FOUND)")
    void findUsername_notFound() {
        given(jwtProvider.getVerifiedEmail(EMAIL_TOKEN)).willReturn(EMAIL);
        given(userRepository.findByEmailAndProvider(EMAIL, Provider.LOCAL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.findUsername(EMAIL_TOKEN))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("아이디 찾기 실패 — 이메일 인증 토큰 무효(EMAIL_NOT_VERIFIED)")
    void findUsername_invalidToken() {
        given(jwtProvider.getVerifiedEmail(EMAIL_TOKEN)).willThrow(new JwtException("bad"));

        assertThatThrownBy(() -> authService.findUsername(EMAIL_TOKEN))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);
    }

    private ResetPasswordRequest resetRequest(String emailToken, String newPassword) {
        ResetPasswordRequest req = new ResetPasswordRequest();
        ReflectionTestUtils.setField(req, "emailVerificationToken", emailToken);
        ReflectionTestUtils.setField(req, "newPassword", newPassword);
        return req;
    }

    @Test
    @DisplayName("비밀번호 재설정 성공 — 이메일 인증 토큰으로 계정을 찾아 새 비밀번호로 교체")
    void resetPassword_success() {
        User user = User.ofLocal(EMAIL, "tester1", passwordEncoder.encode("oldpassword"), "닉");
        given(jwtProvider.getVerifiedEmail(EMAIL_TOKEN)).willReturn(EMAIL);
        given(userRepository.findByEmailAndProvider(EMAIL, Provider.LOCAL)).willReturn(Optional.of(user));

        authService.resetPassword(resetRequest(EMAIL_TOKEN, "newpassword123"));

        assertThat(passwordEncoder.matches("newpassword123", user.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("oldpassword", user.getPassword())).isFalse();
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 — 해당 이메일로 가입한 LOCAL 계정 없음(ACCOUNT_NOT_FOUND)")
    void resetPassword_emailNotRegistered() {
        given(jwtProvider.getVerifiedEmail(EMAIL_TOKEN)).willReturn(EMAIL);
        given(userRepository.findByEmailAndProvider(EMAIL, Provider.LOCAL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword(resetRequest(EMAIL_TOKEN, "newpassword123")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 — 이메일 인증 토큰이 유효하지 않음(EMAIL_NOT_VERIFIED)")
    void resetPassword_invalidEmailToken() {
        given(jwtProvider.getVerifiedEmail(EMAIL_TOKEN)).willThrow(new JwtException("bad"));

        assertThatThrownBy(() -> authService.resetPassword(resetRequest(EMAIL_TOKEN, "newpassword123")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);

        verify(userRepository, never()).findByEmailAndProvider(anyString(), any());
    }
}
