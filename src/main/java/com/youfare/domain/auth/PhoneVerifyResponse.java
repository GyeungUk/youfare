package com.youfare.domain.auth;

import lombok.Builder;
import lombok.Getter;

/** 인증 성공 시 회원가입 요청에 첨부할 단기 phoneVerificationToken을 돌려준다. */
@Getter
@Builder
public class PhoneVerifyResponse {

    private String phoneVerificationToken;
}
