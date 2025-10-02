package com.codelab.micproject.payment.portone;

import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.booking.domain.Appointment;
import com.codelab.micproject.booking.domain.AppointmentStatus;
import com.codelab.micproject.booking.repository.AppointmentRepository;
import com.codelab.micproject.payment.domain.Order;
import com.codelab.micproject.payment.domain.OrderStatus;
import com.codelab.micproject.payment.domain.Payment;
import com.codelab.micproject.payment.domain.PaymentStatus;
import com.codelab.micproject.payment.domain.Refund;
import com.codelab.micproject.payment.domain.RefundStatus;
import com.codelab.micproject.payment.repository.OrderRepository;
import com.codelab.micproject.payment.repository.PaymentRepository;
import com.codelab.micproject.payment.repository.RefundRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortOneWebhookService {

    private final PortOneClient portOneClient;
    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;     // 사용
    private final RefundRepository refundRepository;   // 사용

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Transactional
    public void handle(Map<String, Object> payload) {
        final String impUid = str(payload.get("imp_uid"));
        final String merchantUid = str(payload.get("merchant_uid"));
        if (merchantUid == null) {
            log.warn("Webhook payload without merchant_uid: {}", payload);
            return;
        }

        // 락을 잡아 멱등/경합 방지
        Payment pay = paymentRepository.findWithLockByMerchantUid(merchantUid).orElse(null);
        if (pay == null) {
            log.warn("Payment not found. merchantUid={}", merchantUid);
            return;
        }

        // PortOne 서버-서버 재검증
        String token = portOneClient.getAccessToken();
        Map<String, Object> info = portOneClient.getPaymentByImpUid(token, impUid);
        String verifiedStatus = str(info.get("status"));          // paid/cancelled/refunded...
        BigDecimal paidAmount = toBigDecimal(info.get("amount"));
        String pgProvider   = str(info.get("pg_provider"));
        String payMethod    = str(info.get("pay_method"));
        String receiptUrl   = str(info.get("receipt_url"));

        if (eq(verifiedStatus, "paid")) {
            handlePaid(pay, impUid, paidAmount, pgProvider, payMethod, receiptUrl);
            return;
        }

        if (eq(verifiedStatus, "cancelled") || eq(verifiedStatus, "refunded")) {
            handleRefunded(pay);
            return;
        }

        if (eq(verifiedStatus, "failed")) {
            pay.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(pay);
            log.info("Payment marked FAILED. merchantUid={}", merchantUid);
            return;
        }

        log.info("Unhandled PortOne status: {} (merchantUid={})", verifiedStatus, merchantUid);
    }

    private void handlePaid(Payment pay,
                            String impUid,
                            BigDecimal paidAmount,
                            String pgProvider,
                            String payMethod,
                            String receiptUrl) {

        if (pay.getStatus() == PaymentStatus.PAID && pay.getAppointmentId() != null) {
            log.info("Already PAID and appointment linked. merchantUid={}", pay.getMerchantUid());
            return;
        }

        // 금액 대조
        if (pay.getAmount() == null || paidAmount == null || pay.getAmount().compareTo(paidAmount) != 0) {
            log.error("Amount mismatch. expected={}, paid={}, merchantUid={}",
                    pay.getAmount(), paidAmount, pay.getMerchantUid());
            pay.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(pay);
            return;
        }

        // ✅ 슬롯 최종 겹침 체크 (REQUESTED/APPROVED만 차단, CANCELLED 허용)
        User consultant = userRepository.findById(pay.getConsultantId()).orElseThrow();
        var blocking = List.of(AppointmentStatus.REQUESTED, AppointmentStatus.APPROVED);

        boolean overlap = appointmentRepository.existsActiveOverlap(
                    consultant.getId(),
                    pay.getStartAt(),
                    pay.getEndAt(),
                    null,
                    blocking
        );
        if (overlap) {
            log.error("Overlap detected at approve stage. merchantUid={}", pay.getMerchantUid());
            pay.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(pay);
            return;
        }

        // Appointment 생성(멱등)
        Appointment apt;
        if (pay.getAppointmentId() != null) {
            apt = appointmentRepository.findById(pay.getAppointmentId()).orElse(null);
        } else {
            apt = Appointment.builder()
                    .consultant(consultant)
                    .user(userRepository.findById(pay.getUserId()).orElseThrow())
                    .startAt(pay.getStartAt())
                    .endAt(pay.getEndAt())
                    .status(AppointmentStatus.APPROVED)
                    .meetingUrl("https://meet.example.com/" + pay.getMerchantUid()) // TODO: OpenVidu
                    .build();
            appointmentRepository.save(apt);
            pay.setAppointmentId(apt.getId());
        }

        // Payment 업데이트
        pay.setStatus(PaymentStatus.PAID);
        pay.setImpUid(impUid);
        pay.setPgProvider(pgProvider);
        pay.setPayMethod(payMethod);
        pay.setReceiptUrl(receiptUrl);
        paymentRepository.save(pay);

        // Order 있으면 동기화 (선택)
        Order order = pay.getOrder();
        if (order != null) {
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
        }

        log.info("Payment PAID & Appointment APPROVED. merchantUid={}, aptId={}",
                pay.getMerchantUid(), pay.getAppointmentId());
    }

    private void handleRefunded(Payment pay) {
        if (pay.getStatus() == PaymentStatus.REFUNDED) {
            log.info("Already refunded. merchantUid={}", pay.getMerchantUid());
            return;
        }

        pay.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(pay);

        // Appointment 취소 (→ CANCELLED 이므로 다시 예약 가능)
        if (pay.getAppointmentId() != null) {
            appointmentRepository.findById(pay.getAppointmentId()).ifPresent(a -> {
                a.setStatus(AppointmentStatus.CANCELLED);
                a.setMeetingUrl(null);
                appointmentRepository.save(a);
            });
        }

        // Order/Refund 동기화 (연관 사용)
        Order order = pay.getOrder();
        if (order != null) {
            order.setStatus(OrderStatus.REFUNDED);
            orderRepository.save(order);

            boolean exists = refundRepository.existsByOrderId(order.getId());
            if (!exists) {
                Refund refund = Refund.builder()
                        .order(order)
                        .amount(pay.getAmount())
                        .status(RefundStatus.COMPLETED)
                        .reason("PORTONE_WEBHOOK")
                        .createdAt(OffsetDateTime.now(KST))
                        .completedAt(OffsetDateTime.now(KST))
                        .build();
                refundRepository.save(refund);
            }
        }

        log.info("Payment REFUNDED synced. merchantUid={}", pay.getMerchantUid());
    }

    // utils
    private static String str(Object o) { return o == null ? null : String.valueOf(o); }
    private static boolean eq(String a, String bLower) { return a != null && a.equalsIgnoreCase(bLower); }
    private static BigDecimal toBigDecimal(Object amount) {
        if (amount == null) return null;
        if (amount instanceof BigDecimal bd) return bd;
        if (amount instanceof Number n) return new BigDecimal(n.toString());
        return new BigDecimal(String.valueOf(amount));
    }
}
