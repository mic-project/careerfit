// src/main/java/com/codelab/micproject/payment/service/CheckoutService.java
package com.codelab.micproject.payment.service;

import com.codelab.micproject.account.consultant.domain.ConsultantLevel;
import com.codelab.micproject.account.consultant.repository.ConsultantMetaRepository;
import com.codelab.micproject.account.user.domain.UserRole;
import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.booking.domain.AppointmentStatus; // ✅ 추가
import com.codelab.micproject.booking.dto.SlotDto;
import com.codelab.micproject.booking.repository.AppointmentRepository;
import com.codelab.micproject.booking.service.AvailabilityService;
import com.codelab.micproject.payment.domain.Payment;
import com.codelab.micproject.payment.domain.PaymentStatus;
import com.codelab.micproject.payment.dto.CheckoutRequest;
import com.codelab.micproject.payment.dto.CheckoutResponse;
import com.codelab.micproject.payment.repository.PaymentRepository;
import com.codelab.micproject.security.oauth2.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List; // ✅ 추가

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final ConsultantMetaRepository metaRepository;
    private final PaymentRepository paymentRepository;
    private final AvailabilityService availabilityService;

    @Transactional
    public CheckoutResponse checkout(UserPrincipal me, CheckoutRequest req){
        var user = userRepository.findById(me.id()).orElseThrow();
        var consultant = userRepository.findById(req.consultantId()).orElseThrow();
        if (consultant.getRole() != UserRole.CONSULTANT) {
            throw new IllegalStateException("not a consultant");
        }

        // ✅ 이번 결제에서 사용할 단일 슬롯 하나 뽑기
        var slot = extractSingleSlot(req);
        var startAt = slot.startAt();
        var endAt   = slot.endAt();

        // 1) 가용/간격 검증
        availabilityService.validate(consultant, startAt, endAt);

        // 2) 겹침 체크 (REQUESTED/APPROVED만 차단, CANCELLED 허용)
        var blocking = java.util.EnumSet.of(AppointmentStatus.REQUESTED, AppointmentStatus.APPROVED);
        boolean overlap = appointmentRepository.existsActiveOverlap(
                    consultant.getId(),
                    startAt,
                    endAt,
                    null,
                    blocking
        );
        if (overlap) throw new IllegalArgumentException("SLOT_ALREADY_BOOKED");

        // 3) 금액(1회권) 계산
        var meta  = metaRepository.findByConsultant(consultant).orElse(null);
        var level = (meta != null && meta.getLevel() != null) ? meta.getLevel() : ConsultantLevel.JUNIOR;
        BigDecimal unit =
                (meta != null && meta.getBasePrice() != null)
                        ? meta.getBasePrice()
                        : switch (level) {
                    case JUNIOR    -> BigDecimal.valueOf(30_000);
                    case SENIOR    -> BigDecimal.valueOf(60_000);
                    case EXECUTIVE -> BigDecimal.valueOf(90_000);
                };
        BigDecimal amount = unit;

        // 4) 결제 레코드 (PENDING) — 사전 예약 홀드는 Payment 기반(별도 Appointment 미생성 경로)
        String merchantUid = "order_" + System.currentTimeMillis() + "_" + user.getId();
        var pay = Payment.builder()
                .merchantUid(merchantUid)
                .status(PaymentStatus.PENDING)
                .userId(user.getId())
                .consultantId(consultant.getId())
                .startAt(startAt)
                .endAt(endAt)
                .amount(amount)
                .currency("KRW")
                .payMethod(req.method())
                .build();
        paymentRepository.save(pay);

        // 5) 프론트 결제창 파라미터 반환
        String name      = "1:1 화상면접 1회권";
        String buyerName = (user.getName() != null) ? user.getName() : "회원";
        return new CheckoutResponse(merchantUid, amount, name, buyerName);
    }

    /** 슬롯 배열에서 단일 슬롯을 강제 추출 (1개만 허용) */
    private SlotDto extractSingleSlot(CheckoutRequest req) {
        if (req.slots() == null || req.slots().size() != 1) {
            throw new IllegalArgumentException("slots must contain exactly 1 slot for single checkout");
        }
        return req.slots().get(0);
    }
}
