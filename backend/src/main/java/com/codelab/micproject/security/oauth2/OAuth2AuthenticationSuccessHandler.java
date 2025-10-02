package com.codelab.micproject.security.oauth2;

import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.account.user.domain.UserRole;
import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.auth.service.RefreshTokenService;
import com.codelab.micproject.common.util.CookieUtils;
import com.codelab.micproject.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * OAuth2 성공 시:
 * 1) 토큰 발급
 * 2) RefreshToken DB 저장 (서비스로 위임)
 * 3) 쿠키에 AT/RT 저장
 * 4) 안전한 리다이렉트 (화이트리스트 체크)
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.cookie.domain:}") private String cookieDomain;
    @Value("${app.cookie.secure:false}") private boolean cookieSecure;
    @Value("${app.cookie.same-site:Lax}") private String sameSite;

    @Value("${app.oauth2.success-redirect:http://localhost:5173/oauth2/redirect}")
    private String successRedirect;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication authentication) throws IOException {
        SocialTempPrincipal sp = (SocialTempPrincipal) authentication.getPrincipal();

        // 1) 신규면 즉시 저장(최소필드) - 이메일 중복 체크 추가
        User user = userRepository.findByProviderAndProviderId(sp.getProvider(), sp.getProviderId())
                .orElseGet(() -> {
                    // 이메일이 이미 존재하는지 확인
                    if (sp.getEmail() != null) {
                        Optional<User> existingUser = userRepository.findByEmail(sp.getEmail());
                        if (existingUser.isPresent()) {
                            // 기존 계정에 소셜 정보 연결
                            User existing = existingUser.get();
                            existing.setProvider(sp.getProvider());
                            existing.setProviderId(sp.getProviderId());
                            if (sp.getImageUrl() != null && existing.getProfileImage() == null) {
                                existing.setProfileImage(sp.getImageUrl());
                            }
                            return userRepository.save(existing);
                        }
                    }

                    // 완전히 새로운 사용자 생성
                    return userRepository.save(
                        User.builder()
                                .email(sp.getEmail() != null ? sp.getEmail()
                                        : ("no-email-" + sp.getProvider() + "-" + sp.getProviderId() + "@placeholder.local"))
                                .name(sp.getName() != null ? sp.getName() : sp.getProvider().name() + "_user")
                                .password(null)
                                .role(UserRole.USER)
                                .provider(sp.getProvider())
                                .providerId(sp.getProviderId())
                                .profileImage(sp.getImageUrl())
                                .enabled(true)
                                .build()
                    );
                });

        // 2) 토큰 발급 + 쿠키 세팅
        String access  = tokenProvider.createAccessToken(user);
        String refresh = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime rtExp = LocalDateTime.now().plusDays(tokenProvider.getRefreshExpDays());
        refreshTokenService.replace(user.getId(), refresh, rtExp);

        int accessMaxAge  = (int) (tokenProvider.getAccessExpMin() * 60);
        int refreshMaxAge = (int) (tokenProvider.getRefreshExpDays() * 24 * 60 * 60);

        CookieUtils.addHttpOnlyCookie(res, "ACCESS_TOKEN",  access,  accessMaxAge,  cookieDomain, cookieSecure, sameSite);
        CookieUtils.addHttpOnlyCookie(res, "REFRESH_TOKEN", refresh, refreshMaxAge, cookieDomain, cookieSecure, sameSite);

        // 3) 프론트 리다이렉트(거기서 /MainPage로 이동)
        res.sendRedirect(successRedirect);
    }
}

