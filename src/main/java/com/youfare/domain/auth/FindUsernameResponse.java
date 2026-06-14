package com.youfare.domain.auth;

import lombok.Builder;
import lombok.Getter;

/** 아이디 찾기 응답. 이메일로 가입한 LOCAL 계정의 아이디를 담는다. */
@Getter
@Builder
public class FindUsernameResponse {

    private String username;
}
