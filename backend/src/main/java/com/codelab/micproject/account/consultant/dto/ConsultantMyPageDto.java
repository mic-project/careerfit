package com.codelab.micproject.account.consultant.dto;

import com.codelab.micproject.account.profile.dto.ProfileDto;

public record ConsultantMyPageDto(
        Long id,
        String name,
        String email,
        String phone,
        String company,
        String specialty,
        String introduction,
        Integer careerStartYear,
        Integer careerYears, // computed: Year.now().getValue() - careerStartYear
        String profileImage,
        ProfileDto profile // 기존 ProfileDto 재사용 (bio/skills 등)
) {}