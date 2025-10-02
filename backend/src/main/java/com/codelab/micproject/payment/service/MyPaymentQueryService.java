// com.codelab.micproject.payment.service.MyPaymentQueryService
package com.codelab.micproject.payment.service;

import com.codelab.micproject.payment.domain.OrderStatus;
import com.codelab.micproject.payment.dto.MyPaymentItem;
import com.codelab.micproject.payment.repository.PaymentRepository;
import com.codelab.micproject.payment.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class MyPaymentQueryService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository; // 부분/전액 환불 판단에 사용(있다면)

    public Page<MyPaymentItem> getMyPayments(Long userId, Pageable pageable){
        return paymentRepository.findByUserIdOrderByIdDesc(userId, pageable)
                .map(p -> {
                    var o = p.getOrder();
                    // 취소 가능: 주문이 PAID 이고, 모든 예약 startAt이 오늘(KST) 이후
                    boolean cancellable = false;
                    if (o != null && o.getStatus() == OrderStatus.PAID) {
                        cancellable = o.getAppointments().stream().allMatch(oa ->
                                oa.getAppointment().getStartAt()
                                        .atZoneSameInstant(KST).toLocalDate()
                                        .isAfter(LocalDate.now(KST))
                        );
                    }

                    // 환불 상태(간단 버전): 환불 레코드 존재하면 FULL, 아니면 NONE
                    String refundStatus = refundRepository.existsByOrderId(o.getId())
                            ? "FULL" : "NONE";

                    return new MyPaymentItem(
                            p.getId(),
                            o.getId(),
                            p.getMerchantUid(),
                            p.getAmount(),
                            p.getStatus().name(),
                            o.getBundleCount(),
                            o.getConsultant().getName(),
                            p.getPaidAt(),                // 🔹 paidAt 반환
                            p.getCardBrand(),             // 🔹 카드사
                            p.getCardLast4(),             // 🔹 끝4자리
                            cancellable,
                            refundStatus
                    );
                });
    }
}
