package com.youfare.domain.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 재설정 사전 확인 요청.
 * 이메일 인증 직후, 새 비밀번호를 입력받기 전에 "이 계정이 재설정 가능한지"를 미리 검사하는 데 쓴다.
 * (소셜 계정이면 비밀번호를 헛입력하기 전에 안내하기 위함)
 */
@Getter
@NoArgsConstructor
public class ResetPasswordCheckRequest {

    @NotBlank(message = "이메일 인증이 필요합니다.")
    private String emailVerificationToken;
}
