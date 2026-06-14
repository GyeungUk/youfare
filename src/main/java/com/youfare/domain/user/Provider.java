package com.youfare.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Provider {
    KAKAO("카카오"),
    NAVER("네이버"),
    LOCAL("이메일");

    /** 사용자 안내 메시지에 쓰는 한글 이름 (예: "네이버 로그인으로 가입된 이메일이에요"). */
    private final String displayName;
}
