package com.codelab.micproject.security.jwt;

import com.codelab.micproject.account.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * JJWT 0.12.x 전용 JWT Provider
 * - 프로퍼티 키는 app.jwt.* 우선, 없으면 jwt.* 로 fallback.
 * - HS256 비밀키는 최소 32바이트(UTF-8) 보장.
 * - 액세스 토큰에는 jti 포함(블랙리스트 관리용).
 */
@Component
public class JwtTokenProvider {

    private final String issuer;
    private final long accessExpMin;
    private final long refreshExpDays;
    private final SecretKey signingKey;

    public JwtTokenProvider(
            @Value("${app.jwt.issuer:${jwt.issuer}}") String issuer,
            @Value("${app.jwt.secret:${jwt.secret}}") String secret,
            @Value("${app.jwt.access-exp-min:${jwt.access-exp-min:60}}") long accessExpMin,
            @Value("${app.jwt.refresh-exp-days:${jwt.refresh-exp-days:14}}") long refreshExpDays
    ) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret must be >= 32 bytes (UTF-8).");
        }
        this.issuer = issuer;
        this.accessExpMin = accessExpMin;
        this.refreshExpDays = refreshExpDays;
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /* ==========================
     *  Create Tokens
     * ========================== */

    /** 액세스 토큰 생성 (jti 포함) */
    public String createAccessToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plus(accessExpMin, ChronoUnit.MINUTES);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(user.getId()))
                .id(jti) // ★ jti (블랙리스트용)
                .claim("email", user.getEmail())
                .claim("role", user.getRole() != null ? user.getRole().name() : "USER")
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey)
                .compact();
    }

    /** 리프레시 토큰 생성 (서명+만료만, jti 없음) */
    public String createRefreshToken(Long userId) {
        Instant now = Instant.now();
        Instant exp = now.plus(refreshExpDays, ChronoUnit.DAYS);

        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey)
                .compact();
    }

    /* ==========================
     *  Parse / Extract
     * ========================== */

    /** 0.12.x 파서 (검증 포함) */
    private Claims parseClaims(String token) {
        Jws<Claims> jws = Jwts.parser()               // or parserBuilder()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
        return jws.getPayload();
    }

    /** subject → userId */
    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    /** jti 추출 (액세스 토큰 전용) */
    public String getJti(String token) {
        return parseClaims(token).getId();
    }

    /** 만료 시각(Instant) */
    public Instant getExpiryAt(String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    /** 만료 epoch-second (편의용) */
    public long getExpiryEpochSecond(String token) {
        return getExpiryAt(token).getEpochSecond();
    }

    /** 만료 여부 (현재 시각 기준) */
    public boolean isExpired(String token) {
        return getExpiryAt(token).isBefore(Instant.now());
    }

    /* ==========================
     *  Config Expose
     * ========================== */
    public long getRefreshExpDays() { return refreshExpDays; }
    public long getAccessExpMin()   { return accessExpMin;   }
}
