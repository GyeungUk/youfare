package com.youfare.global.oauth;

import com.youfare.domain.user.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        Map<String, Object> attributes = oAuth2User.getAttributes();
        OAuth2UserInfo userInfo = resolveUserInfo(registrationId, attributes);
        Provider provider = Provider.valueOf(registrationId.toUpperCase());

        // 신규 유저면 저장, 기존 유저면 조회
        User user = userRepository.findBySocialIdAndProvider(userInfo.getSocialId(), provider)
                .orElseGet(() -> {
                    log.info("신규 OAuth2 유저 저장: provider={}, socialId={}", provider, userInfo.getSocialId());
                    return userRepository.save(User.builder()
                            .socialId(userInfo.getSocialId())
                            .provider(provider)
                            .email(userInfo.getEmail())
                            .nickname(userInfo.getNickname())
                            .build());
                });

        // userId를 attribute에 담아서 성공 핸들러에서 꺼낼 수 있게 함
        Map<String, Object> enriched = new java.util.HashMap<>(attributes);
        enriched.put("userId", user.getId());

        return new DefaultOAuth2User(Collections.emptyList(), enriched, userNameAttributeName);
    }

    private OAuth2UserInfo resolveUserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "kakao" -> new KakaoUserInfo(attributes);
            case "naver" -> new NaverUserInfo(attributes);
            default -> throw new OAuth2AuthenticationException("지원하지 않는 provider: " + registrationId);
        };
    }
}
