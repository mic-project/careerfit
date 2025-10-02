package com.codelab.micproject.auth.blacklist;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryBlacklist implements TokenBlacklist {
    private final ConcurrentHashMap<String, Long> map = new ConcurrentHashMap<>();
    @Override public void blacklist(String jti, long ttl) {
        map.put(jti, System.currentTimeMillis() + ttl*1000);
    }
    @Override public boolean isBlacklisted(String jti) {
        Long until = map.get(jti);
        return until != null && System.currentTimeMillis() < until;
    }
}

