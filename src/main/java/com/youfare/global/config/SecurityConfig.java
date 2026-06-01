package com.youfare.global.config;

import com.youfare.domain.user.UserRepository;
import com.youfare.global.jwt.JwtAuthenticationFilter;
import com.youfare.global.jwt.JwtProvider;
import com.youfare.global.oauth.CustomOAuth2UserService;
import com.youfare.global.oauth.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF: REST API는 세션 미사용이므로 비활성화
            .csrf(AbstractHttpConfigurer::disable)

            // 세션 STATELESS — JWT로 인증 관리
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

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
            )

            // JWT 필터: UsernamePasswordAuthenticationFilter 앞에 삽입
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtProvider, userRepository),
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}
