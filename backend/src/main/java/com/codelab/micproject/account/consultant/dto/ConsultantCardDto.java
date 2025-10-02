package com.codelab.micproject.account.consultant.dto;


import java.math.BigDecimal;


public record ConsultantCardDto(Long consultantId, String name, String level, String bio,
                                BigDecimal price, Double avgRating, Long reviewCount){}