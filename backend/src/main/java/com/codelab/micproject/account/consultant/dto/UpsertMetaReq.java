package com.codelab.micproject.account.consultant.dto;


import jakarta.validation.constraints.*;
import java.math.BigDecimal;


public record UpsertMetaReq(
        @NotBlank String level, // JUNIOR/SENIOR/EXECUTIVE
        @PositiveOrZero BigDecimal basePrice // null 허용시 프론트에서 생략 가능
){}