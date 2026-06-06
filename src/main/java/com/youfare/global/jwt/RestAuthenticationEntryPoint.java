package com.youfare.global.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youfare.global.response.ApiResponse;
import com.youfare.global.response.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 미인증 요청에 대한 응답 처리.
 *
 * oauth2Login이 설정된 상태에서 기본 EntryPoint는 보호 자원 접근 시 로그인 페이지로
 * 302 리다이렉트한다. 이 서버는 SPA가 호출하는 REST API이므로, 리다이렉트 대신
 * 401 JSON을 반환해야 클라이언트가 토큰 만료를 정확히 감지하고 재로그인을 유도할 수 있다.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(ErrorCode.UNAUTHORIZED.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(ErrorCode.UNAUTHORIZED));
    }
}
