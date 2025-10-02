package com.codelab.micproject.booking.dto;

public record QuoteResponse(
        Long consultantId,
        String levelName,
        int totalPrice
) {}
