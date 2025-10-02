// com.codelab.micproject.payment.dto.MyPaymentItem
package com.codelab.micproject.payment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MyPaymentItem(
        Long id,                 // payment id
        Long orderId,
        String merchantUid,
        BigDecimal amount,
        String status,           // PAID/REFUNDED/...
        Integer bundleCount,     // 회권수
        String consultantName,
        OffsetDateTime paidAt,   // 🔹 updatedAt 대신 paidAt으로 명시
        String cardBrand,        // 🔹 추가
        String cardLast4,        // 🔹 추가
        Boolean cancellable,     // 🔹 추가: 지금 취소 가능?
        String refundStatus      // 🔹 추가: NONE/REQUESTED/PARTIAL/FULL
) {}
