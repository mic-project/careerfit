package com.codelab.micproject.payment.dto;

public record ConfirmRequest(
        String impUid,
        String merchantUid
) {}