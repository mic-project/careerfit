// com.codelab.micproject.payment.service.MyPageService
package com.codelab.micproject.payment.service;

import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.payment.domain.Order;
import com.codelab.micproject.payment.domain.OrderStatus;
import com.codelab.micproject.payment.domain.Payment;
import com.codelab.micproject.payment.dto.OrderView;
import com.codelab.micproject.payment.repository.OrderRepository;
import com.codelab.micproject.payment.repository.PaymentRepository;
import com.codelab.micproject.payment.repository.RefundRepository;
import com.codelab.micproject.security.oauth2.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MyPageService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;   // ✅ 추가
    private final RefundRepository refundRepository;     // ✅ 추가

    @Transactional(readOnly = true)
    public List<OrderView> myOrders(UserPrincipal me) {
        var user = userRepository.findById(me.id()).orElseThrow();
        var orders = orderRepository.findPaidOrdersByUser(user);
        log.info("🔍 [MyPage] User {} has {} PAID orders", me.id(), orders.size());
        orders.forEach(o -> {
            log.info("  - Order {}: {} appointments", o.getId(), o.getAppointments().size());
            o.getAppointments().forEach(oa -> {
                log.info("    - OrderAppointment {}: Appointment {}", oa.getId(), oa.getAppointment().getId());
            });
        });
        return orders.stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderView> myConsultantOrders(UserPrincipal me) {
        var meUser = userRepository.findById(me.id()).orElseThrow();
        return orderRepository.findByConsultant(meUser).stream().map(this::toView).toList();
    }

    private OrderView toView(Order o) {
        // 예약 요약
        var apps = o.getAppointments().stream()
                .map(oa -> oa.getAppointment())
                .map(a -> new OrderView.AppointmentSummary(
                        a.getId(), a.getStartAt(), a.getEndAt(), a.getStatus().name(), a.getMeetingUrl()
                )).toList();

        // 결제 레코드
        Payment p = paymentRepository.findByOrder(o).orElse(null);

        // 취소 가능: 주문이 PAID이고, 모든 예약 startAt이 '오늘 이후'(KST)
        boolean cancellable = false;
        if (o.getStatus() == OrderStatus.PAID && !o.getAppointments().isEmpty()) {
            LocalDate today = LocalDate.now(KST);
            cancellable = o.getAppointments().stream().allMatch(oa ->
                    oa.getAppointment().getStartAt().atZoneSameInstant(KST).toLocalDate().isAfter(today)
            );
        }

        // 환불 상태(간단 판별): 환불 레코드 존재 → FULL, 아니면 NONE
        String refundStatus = refundRepository.existsByOrderId(o.getId()) ? "FULL" : "NONE";

        return new OrderView(
                o.getId(),
                o.getConsultant().getId(),
                o.getConsultant().getName(),
                o.getBundleCount(),
                o.getUnitPrice(),
                o.getTotalPrice(),
                o.getStatus().name(),
                o.getCreatedAt(),
                // 🔻 결제/환불/취소 필드 세팅(결제가 없으면 null/기본값)
                p != null ? p.getPayMethod()   : null,        // paymentMethod
                p != null ? p.getPgProvider()  : null,        // pgProvider
                p != null ? p.getCardBrand()   : null,        // cardBrand
                p != null ? p.getCardLast4()   : null,        // cardLast4
                p != null ? p.getPaidAt()      : null,        // paidAt
                refundStatus,                                 // refundStatus
                cancellable,                                   // cancellable
                // 예약 목록
                apps
        );
    }
}
