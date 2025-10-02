package com.codelab.micproject.auth.blacklist;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 블랙리스트
 *  - 키: bl:access:{jti}
 *  - 값: "1" (의미 없음)
 *  - TTL: 전달받은 초(ttlSeconds)만큼
 */
@Component
@Primary // 동일 타입 빈 충돌 시 Redis 구현을 기본으로 사용
@RequiredArgsConstructor
public class RedisTokenBlacklist implements TokenBlacklist {

    private static final String KEY_PREFIX = "bl:access:";
    private final StringRedisTemplate redis;

    @Override
    public void blacklist(String jti, long ttlSeconds) {
        if (jti == null || ttlSeconds <= 0) return;
        redis.opsForValue().set(KEY_PREFIX + jti, "1", ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean isBlacklisted(String jti) {
        if (jti == null) return false;
        Boolean exists = redis.hasKey(KEY_PREFIX + jti);
        return Boolean.TRUE.equals(exists);
    }
}
