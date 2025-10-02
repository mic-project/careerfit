package com.codelab.micproject.payment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderView(
        Long id,
        Long consultantId,
        String consultantName,
        int bundleCount,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        String status,
        OffsetDateTime createdAt,
        // 🔻 추가
        String paymentMethod,     // "CARD"
        String pgProvider,        // "html5_inicis" 등
        String cardBrand,         // "삼성카드" 또는 "삼성"
        String cardLast4,         // "4929"
        OffsetDateTime paidAt,    // 결제 완료 시각
        String refundStatus,      // NONE/REQUESTED/PARTIAL/FULL
        boolean cancellable,      // 지금 취소 가능?
        // 예약 목록
        List<OrderView.AppointmentSummary> appointments
) {
    public record AppointmentSummary(
            Long id,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String status,
            String meetingUrl
    ) {}
}
