package com.codelab.micproject.account.user.dto;

import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.account.user.domain.UserRole;

import java.time.LocalDate;

public record UserResponse(
        Long id,
        String email,
        String name,
        String phone,
        String profileImage,
        Integer ticketCount,
        LocalDate ticketStartDate,
        LocalDate ticketEndDate,
        UserRole role
) {
    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getName(),
                u.getPhone(),
                u.getProfileImage(),
                u.getTicketCount(),
                u.getTicketStartDate(),
                u.getTicketEndDate(),
                u.getRole()
        );
    }
}