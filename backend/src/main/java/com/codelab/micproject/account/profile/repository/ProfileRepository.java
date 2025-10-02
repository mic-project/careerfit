package com.codelab.micproject.account.profile.repository;


import com.codelab.micproject.account.profile.domain.Profile;
import com.codelab.micproject.account.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;


public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByUser(User user);
}