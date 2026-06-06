package com.youfare.domain.auth;

import lombok.Builder;
import lombok.Getter;

/** 아이디 찾기 결과 — 전화 인증으로 본인을 확인했으므로 가입 이메일 전체를 돌려준다. */
@Getter
@Builder
public class FindEmailResponse {

    private String email;
}
