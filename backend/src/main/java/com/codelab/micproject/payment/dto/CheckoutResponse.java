package com.codelab.micproject.payment.dto;

public record CheckoutResponse(
        String merchantUid,
        java.math.BigDecimal amount,
        String name,
        String buyerName
) {}
