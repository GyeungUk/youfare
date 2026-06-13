package com.youfare.domain.auth;

import lombok.Builder;
import lombok.Getter;

/** 인증번호 확인 응답. 회원가입·비밀번호 재설정에 사용할 단기 토큰을 담는다. */
@Getter
@Builder
public class EmailVerifyResponse {

    private String emailVerificationToken;
}
