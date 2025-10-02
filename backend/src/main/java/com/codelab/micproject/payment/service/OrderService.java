package com.codelab.micproject.payment.service;

import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.booking.domain.AppointmentStatus;
import com.codelab.micproject.payment.domain.*;
import com.codelab.micproject.payment.repository.*;
import com.codelab.micproject.security.oauth2.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service @RequiredArgsConstructor
public class OrderService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    // private final PaymentRepository paymentRepository;   // ← 임시 미사용
    // private final RefundRepository refundRepository;

    /** 사용자 본인이 주문 취소 */
    @Transactional
    public void cancelMyOrder(UserPrincipal me, Long orderId, String reason) {
        var user = userRepository.findById(me.id()).orElseThrow();
        var order = orderRepository.findById(orderId).orElseThrow();
        if (!order.getUser().getId().equals(user.getId()))
            throw new IllegalStateException("not your order");

        // A안에서는 결제 전 취소만 간단히 허용(예시)
        order.setStatus(OrderStatus.CANCELED);
        order.getAppointments().forEach(oa ->
                oa.getAppointment().setStatus(AppointmentStatus.CANCELLED));

        // TODO: B안(주문/환불) 붙일 때 여기서 payment/ refund 처리 복구
    }

    @Transactional
    public void cancelAsConsultant(UserPrincipal me, Long orderId, String reason) {
        var consultant = userRepository.findById(me.id()).orElseThrow();
        var order = orderRepository.findById(orderId).orElseThrow();
        if (!order.getConsultant().getId().equals(consultant.getId()))
            throw new IllegalStateException("not your order");

        order.setStatus(OrderStatus.CANCELED);
        order.getAppointments().forEach(oa ->
                oa.getAppointment().setStatus(AppointmentStatus.CANCELLED));
        // 환불/정책 추가 필요 시 여기 확장
    }

}