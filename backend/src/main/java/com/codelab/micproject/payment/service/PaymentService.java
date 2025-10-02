package com.codelab.micproject.payment.service;

import com.codelab.micproject.account.consultant.domain.ConsultantLevel;
import com.codelab.micproject.account.consultant.repository.ConsultantMetaRepository;
import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.account.user.domain.UserRole;
import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.booking.domain.Appointment;
import com.codelab.micproject.booking.domain.AppointmentStatus;
import com.codelab.micproject.booking.dto.SlotDto;
import com.codelab.micproject.booking.repository.AppointmentRepository;
import com.codelab.micproject.booking.repository.AvailabilityRepository;
import com.codelab.micproject.booking.repository.AvailableSlotRepository;
import com.codelab.micproject.payment.domain.*;
import com.codelab.micproject.payment.dto.CheckoutRequest;
import com.codelab.micproject.payment.dto.CheckoutResponse;
import com.codelab.micproject.payment.dto.ConfirmRequest;
import com.codelab.micproject.payment.dto.PaymentWebhook;
import com.codelab.micproject.payment.gateway.PortOnePaymentGateway;
import com.codelab.micproject.payment.portone.PortOneClient;
import com.codelab.micproject.payment.repository.OrderAppointmentRepository;
import com.codelab.micproject.payment.repository.OrderRepository;
import com.codelab.micproject.payment.repository.PaymentRepository;
import com.codelab.micproject.payment.repository.RefundRepository;
import com.codelab.micproject.payment.support.PricingPolicy;
import com.codelab.micproject.security.oauth2.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final UserRepository userRepository;
    private final ConsultantMetaRepository metaRepository;
    private final AppointmentRepository appointmentRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final OrderAppointmentRepository orderAppointmentRepository;

    private final AvailabilityRepository availabilityRepository;     // 컨설턴트 TZ 확인용
    private final AvailableSlotRepository availableSlotRepository;   // ★ 정확 일치 검증용

    private final PortOneClient portOneClient;                  // PortOne REST (토큰/조회/취소)
    private final PortOnePaymentGateway portOneGateway;         // issueMerchantUid(), verify() 용

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** PortOne REST 호출용 */
    private final WebClient portone = WebClient.builder()
            .baseUrl("https://api.iamport.kr")
            .build();

    /** 체크아웃: 단일 1회권만 허용 */
    @Transactional
    public CheckoutResponse checkout(UserPrincipal me, CheckoutRequest req) {
        User user = userRepository.findById(me.id()).orElseThrow();
        User consultant = userRepository.findById(req.consultantId()).orElseThrow();

        if (consultant.getRole() != UserRole.CONSULTANT) {
            throw new IllegalStateException("not a consultant");
        }
        if (req.slots() == null || req.slots().size() != 1) {
            throw new IllegalStateException("only single slot is allowed");
        }
        SlotDto slot = req.slots().get(0);

        // 단가 계산(등급별 1회권) — meta.basePrice 우선, 없으면 정책가
        var meta  = metaRepository.findByConsultant(consultant).orElse(null);
        ConsultantLevel level = (meta != null && meta.getLevel() != null) ? meta.getLevel() : ConsultantLevel.JUNIOR;
        BigDecimal unit = BigDecimal.valueOf(PricingPolicy.unitPrice(level));
        BigDecimal total = unit; // 1회권

        // 주문 생성
        Order order = Order.builder()
                .user(user)
                .consultant(consultant)
                .bundleCount(1)
                .unitPrice(unit)
                .totalPrice(total)
                .status(OrderStatus.CREATED)
                .createdAt(OffsetDateTime.now())
                .build();
        orderRepository.save(order);

        // 예약 생성 전: 가용/겹침 검사
        if (!isValidSlotForConsultant(consultant, slot)) {
            throw new IllegalArgumentException("invalid slot for consultant availability");
        }

        // ✅ CANCELLED 는 겹침에서 제외: 예약을 막는 상태만 고려
        // ✅ CANCELLED 는 겹침에서 제외: 예약을 막는 상태만 고려
        var blocking = List.of(AppointmentStatus.REQUESTED, AppointmentStatus.APPROVED);

        boolean overlap = appointmentRepository.existsActiveOverlap(
                    consultant.getId(),
                    slot.startAt(),
                    slot.endAt(),
                    null,
                    blocking
        );
        if (overlap) throw new IllegalStateException("time slot already booked");

        Appointment a = Appointment.builder()
                .consultant(consultant)
                .user(user)
                .startAt(slot.startAt())
                .endAt(slot.endAt())
                .status(AppointmentStatus.REQUESTED)
                .build();
        appointmentRepository.save(a);

        orderAppointmentRepository.save(
                OrderAppointment.builder().order(order).appointment(a).build()
        );

        // merchantUid 발급
        String merchantUid = portOneGateway.issueMerchantUid(order.getId());

        // 결제 레코드(PENDING)
        Payment pay = Payment.builder()
                .merchantUid(merchantUid)
                .order(order)
                .method(req.method())
                .status(PaymentStatus.PENDING)
                .amount(total)
                .userId(user.getId())
                .consultantId(consultant.getId())
                .createdAt(OffsetDateTime.now())
                .build();
        paymentRepository.save(pay);

        String name      = "[" + level.name() + "] 1회권 (" + consultant.getName() + ")";
        String buyerName = Optional.ofNullable(user.getName()).orElse("회원");

        return new CheckoutResponse(merchantUid, total, name, buyerName);
    }

    /** 결제 성공 콜백 → 서버 검증 → 예약 자동 APPROVED */
    @Transactional
    public void confirm(ConfirmRequest req) {
        Payment payment = paymentRepository.findWithLockByMerchantUid(req.merchantUid())
                .orElseThrow(() -> new IllegalArgumentException("payment not found"));
        Order order = payment.getOrder();

        int expectedAmount = (order != null)
                ? order.getTotalPrice().intValue()
                : payment.getAmount().intValue();

        boolean ok = portOneGateway.verify(req.impUid(), req.merchantUid(), expectedAmount);
        if (!ok) {
            log.warn("Payment verification failed: merchantUid={}, impUid={}, expectedAmount={}",
                    req.merchantUid(), req.impUid(), expectedAmount);
            throw new IllegalStateException("payment verification failed");
        }

        // ── PortOne 상세 조회 ──
        String token = portOneClient.getAccessToken();
        Map<String, Object> info = portOneClient.getPaymentByImpUid(token, req.impUid());

        // ⚠️ 전체 카드번호 저장/로그 금지
        String cardName   = (String) info.get("card_name");    // ex) "삼성카드"
        String cardNumber = (String) info.get("card_number");  // ex) "1234-****-****-4929"
        String last4Masked = com.codelab.micproject.common.logging.MaskingUtil.maskCardNumber(cardNumber);
        String last4 = last4Masked != null && last4Masked.length() >= 4
                ? last4Masked.substring(last4Masked.length() - 4) : null;
        Number paidAtEpoch = (Number) info.get("paid_at");

        payment.setStatus(PaymentStatus.PAID);
        payment.setImpUid(req.impUid());
        payment.setPgTransactionId(req.impUid());
        payment.setCardBrand(cardName);
        payment.setCardLast4(last4);
        if (paidAtEpoch != null && paidAtEpoch.longValue() > 0) {
            payment.setPaidAt(OffsetDateTime.ofInstant(
                    Instant.ofEpochSecond(paidAtEpoch.longValue()),
                    ZoneId.of("Asia/Seoul")));
        }
        payment.setPayMethod((String) info.get("pay_method"));
        payment.setPgProvider((String) info.get("pg_provider"));
        payment.setReceiptUrl((String) info.get("receipt_url"));

        log.info("Payment confirmed: merchantUid={}, impUid={}, amount={}, brand={}, last4={}, paidAt={}",
                req.merchantUid(), req.impUid(), expectedAmount, cardName, last4, payment.getPaidAt());

        if (order != null) {
            order.setStatus(OrderStatus.PAID);
            order.getAppointments().forEach(oa -> {
                var a = oa.getAppointment();
                a.setStatus(AppointmentStatus.APPROVED);
                a.setMeetingUrl("https://meet.example.com/" + a.getId());
            });
        }
    }

    @Transactional
    public void webhook(PaymentWebhook webhook) {
        String impUid = webhook.impUid();
        String merchantUid = webhook.merchantUid();

        String token = portOneClient.getAccessToken();
        Map<String, Object> p = portOneClient.getPaymentByImpUid(token, impUid);
        String verifiedStatus = String.valueOf(p.get("status")); // paid, cancelled 등

        Payment payment = paymentRepository.findByMerchantUid(merchantUid).orElse(null);
        if (payment == null) return;

        if ("paid".equalsIgnoreCase(verifiedStatus)) {
            payment.setStatus(PaymentStatus.PAID);
            // 필요 시 cardBrand/cardLast4/paidAt 채우기
        }

        if ("cancelled".equalsIgnoreCase(verifiedStatus) || "refunded".equalsIgnoreCase(verifiedStatus)) {
            payment.setStatus(PaymentStatus.REFUNDED);

            Order order = payment.getOrder();
            if (order != null) {
                order.setStatus(OrderStatus.REFUNDED);
                order.getAppointments().forEach(oa -> {
                    var a = oa.getAppointment();
                    a.setStatus(AppointmentStatus.CANCELLED);
                    a.setMeetingUrl(null);
                });
                boolean exists = refundRepository.existsByOrderId(order.getId());
                if (!exists) {
                    Refund refund = Refund.builder()
                            .order(order)
                            .amount(payment.getAmount())
                            .status(RefundStatus.COMPLETED)
                            .reason("PORTONE_WEBHOOK")
                            .createdAt(OffsetDateTime.now(KST))
                            .completedAt(OffsetDateTime.now(KST))
                            .build();
                    refundRepository.save(refund);
                }
            }
        }
    }

    // ========= 검증/취소 유틸 =========

    /** 컨설턴트의 타임존 결정: availability.zoneId 우선, 없으면 KST, 그래도 없으면 시스템 기본 */
    private ZoneId resolveConsultantZone(User consultant) {
        List<com.codelab.micproject.booking.domain.Availability> list =
                availabilityRepository.findByConsultant(consultant);
        if (!list.isEmpty()) {
            String z = list.get(0).getZoneId();
            if (z != null && !z.isBlank()) return ZoneId.of(z);
        }
        return KST != null ? KST : ZoneId.systemDefault();
    }

    /** 가용시간 검증: available_slots “정확 일치” 존재 여부로 판정 */
    private boolean isValidSlotForConsultant(User consultant, SlotDto s) {
        ZoneId zone = resolveConsultantZone(consultant);

        // OffsetDateTime(UTC기준 시각)을 컨설턴트 TZ의 LocalDateTime으로 변환
        LocalDateTime startLocal = LocalDateTime.ofInstant(s.startAt().toInstant(), zone);
        LocalDateTime endLocal   = LocalDateTime.ofInstant(s.endAt().toInstant(),   zone);

        log.info("🔍 [Slot Validation] consultantId={}, zone={}", consultant.getId(), zone);
        log.info("🔍 [Slot Validation] Request slot: startAt={}, endAt={}", s.startAt(), s.endAt());
        log.info("🔍 [Slot Validation] Converted to local: startLocal={}, endLocal={}", startLocal, endLocal);

        boolean exists = availableSlotRepository.existsByConsultantAndStartAtAndEndAt(
                consultant, startLocal, endLocal
        );

        if (!exists) {
            log.warn("⚠️ [Slot Validation] No exact match found in DB");
            // DB에 저장된 슬롯들을 확인
            var allSlots = availableSlotRepository.findByConsultant(consultant);
            log.warn("⚠️ [Slot Validation] Available slots in DB for consultant {}: {}", consultant.getId(), allSlots.size());
            allSlots.stream().limit(5).forEach(slot ->
                log.warn("  - Slot: start={}, end={}", slot.getStartAt(), slot.getEndAt())
            );
        }
        return exists;
    }

    /** D-day(당일) 취소 불가 규칙 */
    private void assertCancelable(Appointment apt) {
        LocalDate today = LocalDate.now(KST);
        LocalDate target = apt.getStartAt().atZoneSameInstant(KST).toLocalDate();

        if (!target.isAfter(today)) {
            throw new IllegalStateException("당일 및 지난 예약은 취소할 수 없습니다.");
        }
        if (apt.getStatus() == AppointmentStatus.CANCELLED || apt.getStatus() == AppointmentStatus.DONE) {
            throw new IllegalStateException("이미 종료/취소된 예약입니다.");
        }
    }

    /** 결제 취소(전액 환불) */
    @Transactional
    public void cancel(UserPrincipal me, Long orderId, String reason) {
        var user = userRepository.findById(me.id()).orElseThrow();
        var order = orderRepository.findById(orderId).orElseThrow();

        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("본인 주문만 취소할 수 있습니다.");
        }
        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("결제 완료된 주문만 취소할 수 있습니다.");
        }

        order.getAppointments().forEach(oa -> assertCancelable(oa.getAppointment()));

        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new IllegalStateException("결제 내역을 찾을 수 없습니다."));

        String token = portOneClient.getAccessToken();
        String impUid = payment.getImpUid();
        String merchantUid = payment.getMerchantUid();
        BigDecimal refundAmount = payment.getAmount();

        portOneClient.cancelPayment(token, impUid, merchantUid, refundAmount, reason);

        payment.setStatus(PaymentStatus.REFUNDED);
        order.setStatus(OrderStatus.REFUNDED);
        order.getAppointments().forEach(oa -> {
            Appointment a = oa.getAppointment();
            a.setStatus(AppointmentStatus.CANCELLED);
            a.setMeetingUrl(null);
        });

        Refund refund = Refund.builder()
                .order(order)
                .amount(refundAmount)
                .status(RefundStatus.COMPLETED)
                .reason(reason)
                .createdAt(OffsetDateTime.now(KST))
                .completedAt(OffsetDateTime.now(KST))
                .build();
        refundRepository.save(refund);
    }
}
