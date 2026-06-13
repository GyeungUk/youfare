package com.youfare.domain.auth;

import com.youfare.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "폼(이메일/비밀번호) 회원가입·로그인 및 이메일 인증 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    @Operation(summary = "이메일 인증번호 발급",
            description = "입력한 이메일로 6자리 인증번호를 발송합니다.")
    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<EmailSendResponse>> sendEmailCode(
            @Valid @RequestBody EmailSendRequest request) {
        int ttlSeconds = emailVerificationService.issueCode(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok(EmailSendResponse.builder()
                .sent(true)
                .ttlSeconds(ttlSeconds)
                .build()));
    }

    @Operation(summary = "이메일 인증번호 확인",
            description = "인증번호가 맞으면 회원가입·비밀번호 재설정에 사용할 단기 emailVerificationToken을 반환합니다.")
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<EmailVerifyResponse>> verifyEmailCode(
            @Valid @RequestBody EmailVerifyRequest request) {
        String token = emailVerificationService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(ApiResponse.ok(EmailVerifyResponse.builder()
                .emailVerificationToken(token)
                .build()));
    }

    @Operation(summary = "회원가입",
            description = "약관 동의·이메일 인증을 마친 뒤 이메일/비밀번호/닉네임으로 가입하고 accessToken을 발급받습니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<TokenResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(authService.signup(request)));
    }

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하고 accessToken을 발급받습니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @Operation(summary = "비밀번호 재설정",
            description = "이메일 인증을 마친 토큰으로 가입한 계정을 찾아 새 비밀번호로 교체합니다.")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.<Void>ok());
    }
}
