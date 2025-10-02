package com.codelab.micproject.auth.web;

import com.codelab.micproject.account.user.domain.AuthProvider;
import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.account.user.domain.UserRole;
import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.auth.service.RefreshTokenService;
import com.codelab.micproject.common.util.CookieUtils;
import com.codelab.micproject.common.util.SignedCookieUtils;
import com.codelab.micproject.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 역할
 * - 최초 소셜 인증 성공 후 SuccessHandler가 발급한 HttpOnly 쿠키(SOCIAL_JOIN)를 서버에서 읽어
 *   최종 회원을 생성하는 엔드포인트.
 * - 클라이언트는 추가 필드(약관 동의, 전화번호 등)만 JSON으로 전달.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class SocialAuthController {

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.jwt.secret}") private String jwtSecret;
    @Value("${app.cookie.domain:}") private String cookieDomain;
    @Value("${app.cookie.secure:false}") private boolean cookieSecure;
    @Value("${app.cookie.same-site:Lax}") private String sameSite;

    // ✅ JSON만 받도록 명시, SOCIAL_JOIN 쿠키가 없을 때도 400으로 명확히 처리
    @PostMapping(value = "/signup/social", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> completeSocial(
            @CookieValue(name = "SOCIAL_JOIN", required = false) String joinCookie,
            @RequestBody @Valid SocialJoinForm form,
            HttpServletResponse res
    ) {
        if (joinCookie == null) {
            return ResponseEntity.badRequest().body("필요한 쿠키(SOCIAL_JOIN)가 없습니다.");
        }

        Map<String, Object> payload = SignedCookieUtils.readSignedJson(joinCookie, jwtSecret);
        if (payload == null) {
            // 위조/만료 → 즉시 쿠키 제거
            CookieUtils.expire(res, "SOCIAL_JOIN", cookieDomain, cookieSecure, sameSite);
            return ResponseEntity.badRequest().body("만료되었거나 위조된 요청입니다.");
        }

        AuthProvider provider = AuthProvider.valueOf((String) payload.get("provider"));
        String providerId     = (String) payload.get("providerId");
        String email          = (String) payload.get("email");
        String name           = (String) payload.get("name");
        String image          = (String) payload.get("image");

        // ✅ 중복 방지 (idempotent 하게)
        var existing = userRepository.findByProviderAndProviderId(provider, providerId).orElse(null);
        if (existing != null) {
            CookieUtils.expire(res, "SOCIAL_JOIN", cookieDomain, cookieSecure, sameSite);
            return ResponseEntity.badRequest().body("이미 가입된 계정입니다.");
        }

        // ✅ 저장
        User user = User.builder()
                .email(email != null ? email : ("no-email-" + provider + "-" + providerId + "@placeholder.local"))
                .name(name != null ? name : provider.name() + "_user")
                .password(null)
                .role(UserRole.USER)
                .provider(provider)
                .providerId(providerId)
                .profileImage(image)
                .enabled(true)
                .build();
        userRepository.save(user);

        // 임시 쿠키 제거
        CookieUtils.expire(res, "SOCIAL_JOIN", cookieDomain, cookieSecure, sameSite);

        // ✅ 토큰 발급 + 쿠키 저장
        String access  = tokenProvider.createAccessToken(user);
        String refresh = UUID.randomUUID().toString().replace("-", "");
        var   rtExp    = LocalDateTime.now().plusDays(tokenProvider.getRefreshExpDays());
        refreshTokenService.replace(user.getId(), refresh, rtExp);

        int accessMaxAge  = (int) (tokenProvider.getAccessExpMin() * 60);
        int refreshMaxAge = (int) (tokenProvider.getRefreshExpDays() * 24 * 60 * 60);

        CookieUtils.addHttpOnlyCookie(res, "ACCESS_TOKEN",  access,  accessMaxAge,  cookieDomain, cookieSecure, sameSite);
        CookieUtils.addHttpOnlyCookie(res, "REFRESH_TOKEN", refresh, refreshMaxAge, cookieDomain, cookieSecure, sameSite);

        return ResponseEntity.ok(Map.of("success", true));
    }

    public record SocialJoinForm(
            @NotBlank String agreeTos, // "Y"
            String phone,
            String nickName
    ) {}
}

