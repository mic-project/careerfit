package com.codelab.micproject.security.jwt;

import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.auth.blacklist.TokenBlacklist;
import com.codelab.micproject.common.util.CookieUtils;
import com.codelab.micproject.security.oauth2.UserPrincipal;
import com.codelab.micproject.account.user.domain.User;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * 흐름:
 * 1) Authorization 헤더(Bearer) 또는 ACCESS_TOKEN 쿠키에서 JWT 추출
 * 2) 토큰 검증 → userId 파싱 → DB 조회
 * 3) UserPrincipal 생성 후 SecurityContext에 인증 객체 저장
 *    (권한은 UserPrincipal.getAuthorities()에서 ROLE_XXX로 제공)
 * 4) 실패/만료 시 인증 없이 다음 필터 진행 (401은 컨트롤러/시큐리티가 판단)
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final TokenBlacklist blacklist;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String requestPath = req.getRequestURI();

        try {
            String token = resolveToken(req);
            logger.info("🔍 [JWT Filter] Path: " + requestPath + ", Token found: " + (token != null));

            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 1) jti 블랙리스트 먼저 (Redis 실패 시 스킵)
                try {
                    String jti = tokenProvider.getJti(token);
                    if (jti != null && blacklist.isBlacklisted(jti)) {
                        logger.warn("⚠️ [JWT Filter] Token blacklisted: " + jti);
                        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        return; // 바로 차단
                    }
                } catch (Exception redisEx) {
                    logger.warn("⚠️ [JWT Filter] Blacklist check failed (Redis unavailable), continuing: " + redisEx.getMessage());
                    // Redis 연결 실패 시에도 인증 계속 진행
                }

                // 2) 유저 로드
                Long userId = null;
                try {
                    userId = tokenProvider.getUserId(token);
                    logger.info("🔍 [JWT Filter] UserId from token: " + userId);
                } catch (Exception parseEx) {
                    logger.error("❌ [JWT Filter] Failed to parse userId: " + parseEx.getMessage());
                    return;
                }

                if (userId != null) {
                    User user = userRepository.findById(userId).orElse(null);
                    logger.info("🔍 [JWT Filter] User found: " + (user != null) + ", enabled: " + (user != null && user.isEnabled()));

                    if (user != null && user.isEnabled()) {
                        UserPrincipal principal = new UserPrincipal(user, Map.of());
                        var auth = new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        logger.info("✅ [JWT Filter] Authentication set for user: " + userId + ", role: " + user.getRole() + ", authorities: " + principal.getAuthorities());
                    } else {
                        logger.warn("⚠️ [JWT Filter] User not found or disabled for userId: " + userId);
                    }
                } else {
                    logger.warn("⚠️ [JWT Filter] userId is null after parsing token");
                }
            } else if (token == null) {
                logger.warn("⚠️ [JWT Filter] No token found for: " + requestPath);
            }
        } catch (ExpiredJwtException e) {
            logger.warn("⚠️ [JWT Filter] Token expired: " + e.getMessage());
            // 액세스 토큰 만료는 여기서 막지 않고, 컨트롤러에서 /api/auth/refresh 사용 유도
        } catch (Exception e) {
            logger.error("❌ [JWT Filter] Error: " + e.getMessage(), e);
            // 파싱/DB 조회 실패 등은 인증없이 진행
        }
        chain.doFilter(req, res);
    }

    private String resolveToken(HttpServletRequest req) {
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return CookieUtils.getCookieValue(req, "ACCESS_TOKEN").orElse(null);
    }
}
