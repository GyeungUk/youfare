package com.youfare.global.oauth;

import java.util.Map;

/** 카카오/네이버 응답 구조가 다르기 때문에 공통 인터페이스로 추상화 */
public interface OAuth2UserInfo {
    String getSocialId();
    String getEmail();
    String getNickname();
}
