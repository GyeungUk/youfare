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
