// com.codelab.micproject.config.DevAdminInitializer.java
package com.codelab.micproject.config;

import com.codelab.micproject.account.user.domain.AuthProvider;
import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.account.user.domain.UserRole;
import com.codelab.micproject.account.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile({"local","dev"})
@RequiredArgsConstructor
public class DevAdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        userRepository.findByEmail("admin@mic.local").ifPresentOrElse(
                u -> {},
                () -> {
                    User admin = User.builder()
                            .email("admin@mic.local")
                            .name("관리자")
                            .password(passwordEncoder.encode("admin1234!"))
                            .role(UserRole.ADMIN)
                            .provider(AuthProvider.LOCAL)  // ★ 중요
                            .providerId(null)              // 또는 "local"
                            .enabled(true)
                            .build();
                    userRepository.save(admin);
                }
        );
    }
}

