package com.youfare.domain.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 아이디(이메일) 찾기 — 전화 인증을 마친 단기 토큰으로 본인 확인 후 가입 이메일을 조회한다. */
@Getter
@NoArgsConstructor
public class FindEmailRequest {

    @NotBlank(message = "전화번호 인증이 필요합니다.")
    private String phoneVerificationToken;
}
