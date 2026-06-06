package com.youfare.global.oauth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * OAuth2 인증요청(state·redirect 등)을 서버 세션(JSESSIONID) 대신 <b>쿠키</b>에 저장한다.
 *
 * <p>왜 필요한가: SecurityConfig가 {@code SessionCreationPolicy.STATELESS}라서
 * 기본 저장소(HttpSession 기반)는 카카오로 갔다 오는 사이 세션이 유지되지 않아
 * 콜백에서 {@code authorization_request_not_found}로 실패한다("로그인이 한 번에 안 됨").
 * 쿠키에 담으면 세션 없이도 state가 왕복해 첫 시도부터 안정적으로 로그인된다.
 */
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String COOKIE_NAME = "oauth2_auth_request";
    // 인증요청은 로그인 왕복 동안만 살아있으면 되므로 3분이면 충분.
    private static final int COOKIE_MAX_AGE_SECONDS = 180;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return getCookie(request)
                .map(cookie -> deserialize(cookie.getValue()))
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        // null이면 흐름이 끝났다는 의미 → 쿠키 제거
        if (authorizationRequest == null) {
            deleteCookie(request, response);
            return;
        }
        addCookie(response, serialize(authorizationRequest), request.isSecure());
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        // 콜백 처리 시점에 response가 있으므로 여기서 바로 쿠키를 정리한다.
        if (authorizationRequest != null) {
            deleteCookie(request, response);
        }
        return authorizationRequest;
    }

    // ---- 쿠키 헬퍼 ----

    private Optional<Cookie> getCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
                .filter(c -> COOKIE_NAME.equals(c.getName()))
                .findFirst();
    }

    private void addCookie(HttpServletResponse response, String value, boolean secure) {
        Cookie cookie = new Cookie(COOKIE_NAME, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        // 터널/배포(https)에선 Secure로, 로컬(http)에선 끄지 않으면 쿠키가 아예 안 실린다.
        cookie.setSecure(secure);
        cookie.setMaxAge(COOKIE_MAX_AGE_SECONDS);
        response.addCookie(cookie);
    }

    private void deleteCookie(HttpServletRequest request, HttpServletResponse response) {
        getCookie(request).ifPresent(existing -> {
            Cookie cookie = new Cookie(COOKIE_NAME, "");
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        });
    }

    // ---- 직렬화 (OAuth2AuthorizationRequest는 Serializable) ----

    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(authorizationRequest);
            oos.flush();
            return Base64.getUrlEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("OAuth2 인증요청 직렬화 실패", e);
        }
    }

    private OAuth2AuthorizationRequest deserialize(String value) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(value);
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                return (OAuth2AuthorizationRequest) ois.readObject();
            }
        } catch (Exception e) {
            // 손상·만료된 쿠키면 그냥 없는 것으로 처리(재로그인 유도).
            return null;
        }
    }
}
