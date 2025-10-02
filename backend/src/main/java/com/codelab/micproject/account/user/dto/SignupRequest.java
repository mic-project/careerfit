package com.codelab.micproject.account.user.dto;


import jakarta.validation.constraints.*;
import java.time.LocalDate;


public record SignupRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min=8,max=100) String password,
        @NotBlank String name,
        @Pattern(regexp = "^[0-9\\-]{9,15}$", message = "전화번호 형식") String phone,
        LocalDate birthDate
){}