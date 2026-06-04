package com.youfare.global.config;

import com.youfare.domain.user.UserRepository;
import com.youfare.global.jwt.JwtAuthenticationFilter;
import com.youfare.global.jwt.JwtProvider;
import com.youfare.global.jwt.RestAuthenticationEntryPoint;
import com.youfare.global.oauth.CustomOAuth2UserService;
import com.youfare.global.oauth.OAuth2FailureHandler;
import com.youfare.global.oauth.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Value("${frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF: REST API는 세션 미사용이므로 비활성화
            .csrf(AbstractHttpConfigurer::disable)

            // SPA(다른 origin)에서의 호출 허용
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 세션 STATELESS — JWT로 인증 관리
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 미인증 요청은 302 리다이렉트 대신 401 JSON 반환 (REST API)
            .exceptionHandling(eh -> eh.authenticationEntryPoint(restAuthenticationEntryPoint))

            // 경로별 인가
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/auth/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/login/**",
                    "/oauth2/**"
                ).permitAll()
                // 관리자 수동 동기화는 외부 API를 대량 호출하므로 공개 금지(인증 필요).
                // 신규 DB는 WelfareDataSeeder가 기동 시 1회 자동 시드하므로 평소 호출할 일은 없다.
                .requestMatchers(HttpMethod.POST, "/admin/welfare/sync").authenticated()
                // 비로그인(게스트) 허용 — 복지 목록/상세 조회 + AI 상담만 공개
                //   ※ 맞춤 추천(/welfare/recommend)은 프로필 기반이라 로그인 필수.
                //     아래 와일드카드(/welfare/*)보다 먼저 선언해야 우선 매칭된다(순서 = 우선순위).
                .requestMatchers(HttpMethod.GET, "/welfare/recommend").authenticated()
                .requestMatchers(HttpMethod.GET, "/welfare", "/welfare/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/chat").permitAll()
                // 커뮤니티 '읽기'는 게스트 공개 — 목록/인기글/상세/댓글 조회.
                //   작성·수정·삭제·좋아요(POST/PUT/DELETE)는 아래 anyRequest 규칙으로 로그인 필요.
                //   ※ "/posts/*"는 한 세그먼트만 매칭하므로 "/posts/{id}"만 열리고 "/posts/{id}/like"(POST)는 영향 없음.
                .requestMatchers(HttpMethod.GET, "/posts", "/posts/hot", "/posts/*", "/posts/*/comments").permitAll()
                // 스크랩·커뮤니티 쓰기 등 나머지는 로그인 필요
                .anyRequest().authenticated()
            )

            // OAuth2 소셜 로그인 설정
            .oauth2Login(oauth -> oauth
                .userInfoEndpoint(ui -> ui.userService(customOAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
                .failureHandler(oAuth2FailureHandler)
            )

            // JWT 필터: UsernamePasswordAuthenticationFilter 앞에 삽입
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtProvider, userRepository),
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendUrl));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
