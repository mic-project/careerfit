package com.codelab.micproject.payment.dto;

import com.codelab.micproject.booking.dto.SessionBundle;
import com.codelab.micproject.booking.dto.SlotDto;

import java.util.List;

public record CheckoutRequest(
        Long consultantId,
        SessionBundle bundle,
        List<SlotDto> slots,
        String method
) {}
