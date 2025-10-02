package com.codelab.micproject.auth.web;

import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.auth.domain.RefreshToken;
import com.codelab.micproject.auth.repository.RefreshTokenRepository;
import com.codelab.micproject.auth.service.RefreshTokenService;
import com.codelab.micproject.common.util.CookieUtils;
import com.codelab.micproject.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class TokenController {

    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    @Value("${app.cookie.domain:}")     private String  cookieDomain;
    @Value("${app.cookie.secure:false}") private boolean cookieSecure;
    @Value("${app.cookie.same-site:Lax}") private String sameSite;

    /** RT가 유효하면 AT 재발급 + RT 회전(rotate) */
    @PostMapping("/refresh")
    @Transactional
    public ResponseEntity<Void> refresh(HttpServletRequest req, HttpServletResponse res) {
        // 0) 쿠키에서 RT 추출
        String rt = CookieUtils.getCookieValue(req, "REFRESH_TOKEN").orElse(null);
        if (rt == null || rt.isBlank()) {
            log.debug("[/refresh] no refresh cookie");
            return unauthorized();
        }

        // 1) RT 유효성 확인
        RefreshToken entity = refreshTokenRepository.findByToken(rt).orElse(null);
        if (entity == null) {
            log.debug("[/refresh] refresh token not found (token rotated or invalid)");
            return unauthorized();
        }
        LocalDateTime now = LocalDateTime.now();
        if (entity.getExpiresAt() == null || entity.getExpiresAt().isBefore(now)) {
            log.debug("[/refresh] refresh token expired");
            return unauthorized();
        }

        // 2) 사용자 로드
        Long userId = entity.getUserId();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isEnabled()) {
            log.warn("[/refresh] user not found or disabled: {}", userId);
            return unauthorized();
        }

        // 3) 새 AT 생성
        String newAccess = tokenProvider.createAccessToken(user);

        // 4) RT 회전(rotate) : 기존 RT 폐기 후 신규 RT/만료로 교체
        String newRefresh = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime newRtExp = now.plusDays(tokenProvider.getRefreshExpDays());
        refreshTokenService.replace(userId, newRefresh, newRtExp);

        // 5) 쿠키 교체(속성 일관성 유지)
        int accessMaxAgeSec  = (int) (tokenProvider.getAccessExpMin() * 60);
        int refreshMaxAgeSec = (int) (tokenProvider.getRefreshExpDays() * 24 * 3600);
        CookieUtils.addHttpOnlyCookie(res, "ACCESS_TOKEN",  newAccess,  accessMaxAgeSec,  cookieDomain, cookieSecure, sameSite);
        CookieUtils.addHttpOnlyCookie(res, "REFRESH_TOKEN", newRefresh, refreshMaxAgeSec, cookieDomain, cookieSecure, sameSite);

        // 204가 프론트 처리에 안전(바디 없음)
        return ResponseEntity.noContent().build();
    }

    /* -------------------- helpers -------------------- */

    private ResponseEntity<Void> unauthorized() {
        // 401 + 바디 없음 (프론트는 /api/auth/refresh 실패 시 재로그인 유도)
        return ResponseEntity.status(401).build();
    }
}
