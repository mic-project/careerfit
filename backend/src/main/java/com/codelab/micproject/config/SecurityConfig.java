// src/main/java/com/codelab/micproject/config/SecurityConfig.java
package com.codelab.micproject.config;

import com.codelab.micproject.security.jwt.JwtAuthenticationFilter;
import com.codelab.micproject.security.oauth2.CustomOAuth2UserService;
import com.codelab.micproject.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.codelab.micproject.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.codelab.micproject.security.web.JsonAccessDeniedHandler;
import com.codelab.micproject.security.web.JsonAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;


@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final CustomOAuth2UserService oAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler successHandler;
    private final OAuth2AuthenticationFailureHandler failureHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           HandlerMappingIntrospector introspector) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s
                        // JWT 기반 인증이므로 STATELESS
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .cors(cors -> {}) // ★ CorsConfig(WebMvcConfigurer) 적용
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll() // ← 추가
                        .requestMatchers("/", "/health", "/api/health", "/error").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        // 정적 리소스 (테스트 페이지 포함)
                        .requestMatchers("/social/**", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers("/index.html", "/index-new.html", "/final.html", "/ultimate.html", "/interview.html", "/test.html", "/video-test.html", "/app.js", "/app-fixed.js", "/app-final.js", "/app-ultimate.js", "/openvidu-browser.min.js").permitAll()
                        .requestMatchers("/static/**").permitAll()
                        // OpenVidu 테스트 API (임시 허용)
                        .requestMatchers("/api/openvidu/**").permitAll()
                        // 인증 관련
                        .requestMatchers("/api/auth/signup").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/check-email").permitAll()
                        .requestMatchers("/api/auth/email/**").permitAll()
                        .requestMatchers("/api/auth/phone/**").permitAll()
                        .requestMatchers("/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers("/api/auth/me").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/consultants/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/consultants/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/portone/webhook").permitAll()
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/uploads/**").permitAll()
                        // 프리플라이트 안전빵 (선택)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(o -> o
                        .userInfoEndpoint(u -> u.userService(oAuth2UserService))
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                )
                // /api/** 는 JSON 401/403, 그 외는 로그인 리다이렉트 유지
                .exceptionHandling(e -> {
            // 모든 요청에 대해 JSON 401/403로 통일
            e.authenticationEntryPoint(new JsonAuthenticationEntryPoint());
            e.accessDeniedHandler(new JsonAccessDeniedHandler());
        });

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
