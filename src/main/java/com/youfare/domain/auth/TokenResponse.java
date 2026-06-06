package com.youfare.domain.auth;

import lombok.Builder;
import lombok.Getter;

/** 소셜 로그인의 ?token=… 콜백과 동일하게 accessToken을 본문으로 반환한다. */
@Getter
@Builder
public class TokenResponse {

    private Long userId;
    private String accessToken;

    public static TokenResponse of(Long userId, String accessToken) {
        return TokenResponse.builder()
                .userId(userId)
                .accessToken(accessToken)
                .build();
    }
}
