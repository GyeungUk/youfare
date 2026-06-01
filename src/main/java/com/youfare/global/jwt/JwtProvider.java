package com.youfare.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 검증 흐름
 * 1. 클라이언트가 Authorization: Bearer <token> 헤더로 요청
 * 2. JwtAuthenticationFilter가 헤더에서 토큰 추출
 * 3. JwtProvider.validateToken()으로 서명·만료 검증
 * 4. 유효하면 getUserId()로 userId 추출 → DB에서 User 엔티티 조회
 * 5. SecurityContext에 Authentication 저장 → 이후 컨트롤러에서 @AuthenticationPrincipal로 접근
 */
@Slf4j
@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secretStr;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secretStr.getBytes(StandardCharsets.UTF_8));
    }

    /** 로그인 성공 시 userId를 subject로 담아 accessToken 발급 */
    public String generateToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    /** 토큰 서명·만료 검증 */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT 만료: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT 유효하지 않음: {}", e.getMessage());
        }
        return false;
    }

    /** 검증된 토큰에서 userId 추출 */
    public Long getUserId(String token) {
        return Long.parseLong(
                Jwts.parser().verifyWith(secretKey).build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .getSubject()
        );
    }
}
