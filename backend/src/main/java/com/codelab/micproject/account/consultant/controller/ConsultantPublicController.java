// src/main/java/com/codelab/micproject/account/consultant/controller/ConsultantPublicController.java
package com.codelab.micproject.account.consultant.controller;

import com.codelab.micproject.account.consultant.dto.ConsultantPublicDto;
import com.codelab.micproject.account.profile.domain.Profile;
import com.codelab.micproject.account.profile.repository.ProfileRepository;
import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.account.user.domain.UserRole;
import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.List;

@RestController
@RequestMapping("/api/public/consultants")
@RequiredArgsConstructor
public class ConsultantPublicController {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    @GetMapping
    public ApiResponse<List<ConsultantPublicDto>> list() {
        List<User> users = userRepository.findByRole(UserRole.CONSULTANT);
        return ApiResponse.ok(users.stream().map(this::toPublic).toList());
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<ConsultantPublicDto> one(@PathVariable Long id) {
        User u = userRepository.findById(id).orElseThrow();
        return ApiResponse.ok(toPublic(u));
    }

    private ConsultantPublicDto toPublic(User u) {
        Profile p = profileRepository.findByUser(u).orElse(null);

        Integer years = null;
        String specialty = null;
        String introduction = null; // ★ 추가

        if (p != null) {
            if (p.getCareerStartYear() != null) {
                years = Year.now().getValue() - p.getCareerStartYear();
            }
            if (p.getSkills() != null && !p.getSkills().isBlank()) {
                String[] parts = p.getSkills().trim().split("[,\\n]");
                if (parts.length > 0) specialty = parts[0].trim();
            }
            if (p.getBio() != null && !p.getBio().isBlank()) {
                introduction = p.getBio().trim();
            }
        }
        return new ConsultantPublicDto(
                u.getId(),
                u.getName(),
                u.getProfileImage(),
                years,
                specialty,
                introduction // ★ 전달
        );
    }
}
