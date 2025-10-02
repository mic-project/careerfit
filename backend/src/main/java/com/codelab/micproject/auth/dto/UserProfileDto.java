// auth/dto/UserProfileDto.java
package com.codelab.micproject.auth.dto;

import com.codelab.micproject.account.user.domain.*;
import lombok.Builder;

@Builder
public record UserProfileDto(
        Long id,
        String email,
        String name,
        String profileImage,
        UserRole role,
        AuthProvider provider
) { }
