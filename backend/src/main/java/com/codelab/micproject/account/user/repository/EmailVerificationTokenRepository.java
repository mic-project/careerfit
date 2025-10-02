package com.codelab.micproject.account.user.repository;

import com.codelab.micproject.account.user.domain.EmailVerificationToken;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update EmailVerificationToken t set t.used = true where t.user.id = :userId and t.used = false")
    void invalidateAllForUser(@Param("userId") Long userId);
}
