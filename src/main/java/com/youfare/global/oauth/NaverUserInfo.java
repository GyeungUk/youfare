package com.youfare.global.oauth;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Map;

/** 네이버 응답 구조: { resultcode, message, response: { id, email, name } } */
public class NaverUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> response;

    @SuppressWarnings("unchecked")
    public NaverUserInfo(Map<String, Object> attributes) {
        Object res = attributes.get("response");
        if (res == null) {
            throw new OAuth2AuthenticationException("네이버 응답에 'response' 필드가 없습니다.");
        }
        this.response = (Map<String, Object>) res;
    }

    @Override
    public String getSocialId() {
        return (String) response.get("id");
    }

    @Override
    public String getEmail() {
        return (String) response.get("email");
    }

    @Override
    public String getNickname() {
        return (String) response.get("name");
    }
}
