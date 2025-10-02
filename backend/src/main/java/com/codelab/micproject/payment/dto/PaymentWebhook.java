package com.codelab.micproject.payment.dto;

public record PaymentWebhook(
        String merchantUid,
        String impUid,
        String status,          // "paid" | "failed" ...
        String pgTransactionId,
        String receiptUrl
) {}
