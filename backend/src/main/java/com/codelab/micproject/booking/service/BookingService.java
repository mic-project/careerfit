package com.codelab.micproject.booking.service;

import com.codelab.micproject.account.consultant.domain.ConsultantLevel;
import com.codelab.micproject.account.consultant.repository.ConsultantMetaRepository;
import com.codelab.micproject.account.user.domain.UserRole;
import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.booking.domain.Appointment;
import com.codelab.micproject.booking.domain.AppointmentStatus;
import com.codelab.micproject.booking.dto.*;
import com.codelab.micproject.booking.repository.AppointmentRepository;
import com.codelab.micproject.booking.support.MeetingLinkFactory;
import com.codelab.micproject.payment.support.PricingPolicy;
import com.codelab.micproject.security.oauth2.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final ConsultantMetaRepository metaRepository;
    private final MeetingLinkFactory meetingLinkFactory;
    private final AvailabilityService availabilityService;

    @Transactional
    public AppointmentView request(UserPrincipal me, RequestAppointment req){
        var user = userRepository.findById(me.id()).orElseThrow();
        var consultant = userRepository.findById(req.consultantId()).orElseThrow();
        if (consultant.getRole() != UserRole.CONSULTANT) throw new IllegalStateException("not a consultant");

        // 1) 가용/간격 검증
        availabilityService.validate(consultant, req.startAt(), req.endAt());

        // 2) 겹침(REQUESTED/APPROVED만 차단)
        var blocking = java.util.EnumSet.of(AppointmentStatus.REQUESTED, AppointmentStatus.APPROVED);
        boolean overlap = appointmentRepository.existsActiveOverlap(
                    consultant.getId(),
                    req.startAt(),
                    req.endAt(),
                    null,              // 생성이므로 제외 id 없음
                    blocking
                        );
        if (overlap) throw new IllegalStateException("time slot already booked");

        // 3) 요청 생성(REQUESTED)
        var a = Appointment.builder()
                .consultant(consultant).user(user)
                .startAt(req.startAt()).endAt(req.endAt())
                .status(AppointmentStatus.REQUESTED)
                .build();
        appointmentRepository.save(a);

        return new AppointmentView(
                a.getId(), consultant.getId(), user.getId(),
                a.getStartAt(), a.getEndAt(), a.getStatus().name(), a.getMeetingUrl()
        );
    }

    /** 새 정책: 1회권만 허용 → 배치 요청은 1개 슬롯만 받아들임 */
    @Transactional
    public List<AppointmentView> requestBatch(UserPrincipal me, RequestAppointmentBatch req){
        if (req.slots() == null || req.slots().size() != 1) {
            throw new IllegalStateException("only single slot is allowed");
        }
        var single = new RequestAppointment(
                req.consultantId(),
                req.slots().get(0).startAt(),
                req.slots().get(0).endAt()
        );
        return List.of(request(me, single));
    }

    @Transactional
    public AppointmentView approve(UserPrincipal me, Long appointmentId){
        var a = appointmentRepository.findById(appointmentId).orElseThrow();
        var meUser = userRepository.findById(me.id()).orElseThrow();
        if (!a.getConsultant().getId().equals(meUser.getId()))
            throw new IllegalStateException("only consultant can approve");
        if (a.getStatus() != AppointmentStatus.REQUESTED)
            throw new IllegalStateException("not requested");

        a.setStatus(AppointmentStatus.APPROVED);
        a.setMeetingUrl(meetingLinkFactory.buildJoinUrl(a));
        return new AppointmentView(
                a.getId(), a.getConsultant().getId(), a.getUser().getId(),
                a.getStartAt(), a.getEndAt(), a.getStatus().name(), a.getMeetingUrl()
        );
    }

    /** 등급 기반 1회권 견적 */
    @Transactional(readOnly = true)
    public QuoteResponse quote(Long consultantId) {
        var consultant = userRepository.findById(consultantId).orElseThrow();
        if (consultant.getRole() != UserRole.CONSULTANT) throw new IllegalStateException("not a consultant");

        var meta = metaRepository.findByConsultant(consultant).orElse(null);
        ConsultantLevel level = (meta != null && meta.getLevel() != null) ? meta.getLevel() : ConsultantLevel.JUNIOR;

        int price = (meta != null && meta.getBasePrice() != null)
                ? meta.getBasePrice().intValue()
                : PricingPolicy.unitPrice(level);

        return new QuoteResponse(consultant.getId(), level.name(), price);
    }
}
