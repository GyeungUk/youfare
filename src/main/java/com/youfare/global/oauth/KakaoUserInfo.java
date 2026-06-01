package com.youfare.global.oauth;

import java.util.Map;

/** 카카오 응답 구조: { id, kakao_account: { email, profile: { nickname } } } */
public class KakaoUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    @SuppressWarnings("unchecked")
    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getSocialId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getEmail() {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        return account != null ? (String) account.get("email") : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getNickname() {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        if (account == null) return null;
        Map<String, Object> profile = (Map<String, Object>) account.get("profile");
        return profile != null ? (String) profile.get("nickname") : null;
    }
}
