package com.codelab.micproject.auth.service;


import com.codelab.micproject.auth.domain.RefreshToken;
import com.codelab.micproject.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository repo;

    @Transactional
    public void replace(Long userId, String token, LocalDateTime expiresAt) {
        repo.deleteByUserId(userId);
        repo.save(RefreshToken.builder()
                .userId(userId)
                .token(token)
                .expiresAt(expiresAt)
                .build());
    }

    @Transactional
    public void deleteAllByUserId(Long userId) {
        repo.deleteByUserId(userId);
    }
}