package com.youfare.global.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth2 소셜 로그인 실패 처리.
 *
 * 기본 동작은 실패 시 /login?error 로 리다이렉트하는데, 이 서버는 SPA + REST 구조라
 * /login 정적 리소스가 없어 404(NoResourceFoundException)가 발생하고 진짜 원인이 가려진다.
 * 이 핸들러는 실패의 실제 원인(카카오 에러 코드/설명)을 로그로 남기고,
 * 사용자는 프론트엔드 로그인 페이지로 깔끔히 되돌려 보낸다.
 */
@Slf4j
@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    @Value("${frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String reason = "unknown";

        if (exception instanceof OAuth2AuthenticationException oae) {
            OAuth2Error error = oae.getError();
            reason = error.getErrorCode();
            log.error("OAuth2 로그인 실패 — errorCode={}, description={}, uri={}",
                    error.getErrorCode(), error.getDescription(), error.getUri(), exception);
        } else {
            log.error("OAuth2 로그인 실패 — {}", exception.getMessage(), exception);
        }

        String redirect = frontendUrl + "/login?error="
                + URLEncoder.encode(reason, StandardCharsets.UTF_8);
        response.sendRedirect(redirect);
    }
}
