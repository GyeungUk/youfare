package com.youfare.global.oauth;

import com.youfare.domain.user.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
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
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 1. 카카오/네이버 userInfo 엔드포인트 호출 (네트워크/토큰 문제 시 여기서 실패)
        OAuth2User oAuth2User;
        try {
            oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);
        } catch (OAuth2AuthenticationException e) {
            log.error("OAuth2 userInfo 조회 실패: provider={}, error={}", registrationId, e.getError(), e);
            throw e;
        }

        // 2. 응답 파싱 + 유저 저장/조회 (파싱·DB 문제 시 OAuth2AuthenticationException으로 변환해 실패 핸들러로 전달)
        try {
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
                                // 이메일은 소문자로 정규화해 저장한다. LOCAL 계정과 동일한 정규화 규칙을 맞춰
                                // 같은 이메일의 소셜/폼 계정 교차 조회(아이디찾기·비번재설정 안내)가 일관되게 매칭되게 한다.
                                .email(normalizeEmail(userInfo.getEmail()))
                                .nickname(userInfo.getNickname())
                                .build());
                    });

            log.info("OAuth2 인증 완료: provider={}, userId={}", provider, user.getId());

            // userId를 attribute에 담아서 성공 핸들러에서 꺼낼 수 있게 함
            Map<String, Object> enriched = new java.util.HashMap<>(attributes);
            enriched.put("userId", user.getId());

            return new DefaultOAuth2User(Collections.emptyList(), enriched, userNameAttributeName);
        } catch (OAuth2AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("OAuth2 유저 처리 실패: provider={}, message={}", registrationId, e.getMessage(), e);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("user_processing_failed", e.getMessage(), null), e);
        }
    }

    /** 대소문자·공백 차이로 같은 메일이 다른 계정처럼 취급되지 않도록 정규화 (AuthService와 동일 규칙). 소셜은 email이 null일 수 있다. */
    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private OAuth2UserInfo resolveUserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "kakao" -> new KakaoUserInfo(attributes);
            case "naver" -> new NaverUserInfo(attributes);
            default -> throw new OAuth2AuthenticationException("지원하지 않는 provider: " + registrationId);
        };
    }
}
