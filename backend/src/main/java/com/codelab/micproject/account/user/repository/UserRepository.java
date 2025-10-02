package com.codelab.micproject.account.user.repository;

import com.codelab.micproject.account.user.domain.AuthProvider;
import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.account.user.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);
    boolean existsByEmail(String email);

    // ✅ 추가: 활성 컨설턴트 전원 조회
    List<User> findByRoleAndEnabled(UserRole role, boolean enabled);

    List<User> findByRole(UserRole role);
}
