package com.youfare.domain.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 비밀번호 재설정 — 이메일 인증 토큰으로 본인을 확인한 뒤 새 비밀번호로 교체한다. */
@Getter
@NoArgsConstructor
public class ResetPasswordRequest {

    @NotBlank(message = "이메일 인증이 필요합니다.")
    private String emailVerificationToken;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 64, message = "비밀번호는 8자 이상이어야 합니다.")
    private String newPassword;
}
