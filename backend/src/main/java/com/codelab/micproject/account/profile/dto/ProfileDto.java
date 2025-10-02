package com.codelab.micproject.account.profile.dto;

import java.math.BigDecimal;

public record ProfileDto(
        Long id,
        String bio,
        String skills,
        String career,
        BigDecimal hourlyRate,   // ← Integer -> BigDecimal
        boolean publicCalendar
) {}
