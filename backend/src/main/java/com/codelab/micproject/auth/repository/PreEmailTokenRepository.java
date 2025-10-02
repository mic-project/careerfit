package com.codelab.micproject.auth.repository;

import com.codelab.micproject.auth.domain.PreEmailToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreEmailTokenRepository extends JpaRepository<PreEmailToken, Long> {
    Optional<PreEmailToken> findTopByEmailOrderByIdDesc(String email);
    Optional<PreEmailToken> findByToken(String token);
    void deleteByEmail(String email);
}
