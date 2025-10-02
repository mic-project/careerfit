package com.codelab.micproject.payment.gateway;

public record PortOnePaymentInfo(
        String impUid, String merchantUid, String status, int amount,
        String cardBrand, String cardLast4, long paidAtEpoch
) {}