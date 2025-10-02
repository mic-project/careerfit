// package: com.codelab.micproject.security.oauth2

package com.codelab.micproject.security.oauth2;

import com.codelab.micproject.auth.domain.RefreshToken;
import com.codelab.micproject.auth.repository.RefreshTokenRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * RefreshToken 저장/교체를 트랜잭션으로 보장하는 서비스.
 * OAuth2/일반 로그인 공통으로 재사용 가능.
 */
@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 유저별 기존 RT를 제거하고 새 RT를 저장.
     * 트랜잭션 경계 안에서 delete → save 순서 보장.
     */
    @Transactional
    public void replaceRefreshToken(Long userId, String refreshToken, LocalDateTime expiresAt) {
        // 동시 로그인/재발급 레이스를 줄이려면 여기서 조건부 삭제/업서트 전략도 가능
        refreshTokenRepository.deleteByUserId(userId);
        refreshTokenRepository.save(RefreshToken.builder()
                .token(refreshToken)
                .userId(userId)
                .expiresAt(expiresAt)
                .build());
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
