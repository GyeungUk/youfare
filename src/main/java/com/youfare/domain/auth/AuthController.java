package com.youfare.domain.auth;

import com.youfare.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "폼(이메일/비밀번호) 회원가입·로그인 및 전화번호 인증 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PhoneVerificationService phoneVerificationService;

    // 데모용 인증번호를 HTTP 응답으로 노출할지 여부. 운영 기본 false.
    //  true로 두면 누구나 "타인 번호로 인증번호 발급 → 인증 → 비밀번호 재설정"으로 계정을 탈취할 수 있다.
    //  운영에선 false 유지하고 실제 SMS를 연동한다. 로컬 데모 편의가 필요하면 환경변수로만 켠다.
    //  (코드 자체는 서버 로그(INFO)에 항상 남아 로컬 운영자는 확인 가능)
    @Value("${auth.phone.expose-dev-code:false}")
    private boolean exposeDevCode;

    @Operation(summary = "전화번호 인증번호 발급(목업)",
            description = "6자리 인증번호를 발급합니다. 운영 기본값에선 devCode를 노출하지 않습니다(보안). "
                    + "auth.phone.expose-dev-code=true 일 때만 응답에 devCode가 포함됩니다.")
    @PostMapping("/phone/send")
    public ResponseEntity<ApiResponse<PhoneSendResponse>> sendPhoneCode(
            @Valid @RequestBody PhoneSendRequest request) {
        String code = phoneVerificationService.issueCode(request.getPhoneNumber());
        return ResponseEntity.ok(ApiResponse.ok(PhoneSendResponse.builder()
                .sent(true)
                .ttlSeconds(180)
                .devCode(exposeDevCode ? code : null) // 운영 기본 미노출 → 원격 계정탈취 차단
                .build()));
    }

    @Operation(summary = "전화번호 인증번호 확인",
            description = "인증번호가 맞으면 회원가입에 사용할 단기 phoneVerificationToken을 반환합니다.")
    @PostMapping("/phone/verify")
    public ResponseEntity<ApiResponse<PhoneVerifyResponse>> verifyPhoneCode(
            @Valid @RequestBody PhoneVerifyRequest request) {
        String token = phoneVerificationService.verifyCode(request.getPhoneNumber(), request.getCode());
        return ResponseEntity.ok(ApiResponse.ok(PhoneVerifyResponse.builder()
                .phoneVerificationToken(token)
                .build()));
    }

    @Operation(summary = "회원가입",
            description = "약관 동의·전화 인증을 마친 뒤 이메일/비밀번호/닉네임으로 가입하고 accessToken을 발급받습니다.")
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

    @Operation(summary = "아이디(이메일) 찾기",
            description = "전화 인증을 마친 토큰으로 본인 확인 후, 가입한 이메일 전체를 반환합니다.")
    @PostMapping("/find-email")
    public ResponseEntity<ApiResponse<FindEmailResponse>> findEmail(
            @Valid @RequestBody FindEmailRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.findEmail(request)));
    }

    @Operation(summary = "비밀번호 재설정",
            description = "전화 인증을 마친 번호로 가입한 계정을 찾아 새 비밀번호로 교체합니다.")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.<Void>ok());
    }
}
