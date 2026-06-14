package com.youfare.domain.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 아이디 찾기 요청. 이메일 인증을 마친 토큰으로 본인 확인 후 아이디를 돌려준다. */
@Getter
@NoArgsConstructor
public class FindUsernameRequest {

    @NotBlank(message = "이메일 인증이 필요합니다.")
    private String emailVerificationToken;
}
