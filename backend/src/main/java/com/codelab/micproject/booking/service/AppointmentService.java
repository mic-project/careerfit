package com.codelab.micproject.booking.service;

import com.codelab.micproject.booking.domain.Appointment;
import com.codelab.micproject.booking.domain.AppointmentStatus;
import com.codelab.micproject.booking.dto.RescheduleRequest;
import com.codelab.micproject.booking.repository.AppointmentRepository;
import com.codelab.micproject.security.oauth2.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.EnumSet;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

    @Transactional
    public void reschedule(Long appointmentId, UserPrincipal me, RescheduleRequest req) {
        var appt = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

        if (appt.getUser() == null || !appt.getUser().getId().equals(me.getId())) {
            throw new SecurityException("본인 예약만 변경할 수 있습니다.");
        }

        // 상태검증
        if (appt.getStatus() != AppointmentStatus.REQUESTED &&
                appt.getStatus() != AppointmentStatus.APPROVED) {
            throw new IllegalStateException("해당 상태에서는 변경할 수 없습니다.");
        }

        var newStart = req.startAt();
        var newEnd   = req.endAt();
        if (newStart == null || newEnd == null || !newEnd.isAfter(newStart)) {
            throw new IllegalArgumentException("시작/종료 시간이 올바르지 않습니다.");
        }
        if (!newStart.isAfter(OffsetDateTime.now())) {
            throw new IllegalArgumentException("과거 시간으로는 변경할 수 없습니다.");
        }

        var ACTIVE = java.util.EnumSet.of(AppointmentStatus.REQUESTED, AppointmentStatus.APPROVED);
        boolean overlap = appointmentRepository.existsActiveOverlap(
                    appt.getConsultant().getId(),
                    newStart,
                    newEnd,
                    appt.getId(),      // 변경이므로 자기 자신 제외
                    ACTIVE
                        );
        if (overlap) throw new IllegalStateException("이미 예약된 시간입니다.");

        appt.setStartAt(newStart);
        appt.setEndAt(newEnd);
        // flush는 트랜잭션 종료 시점에 일어남 (업데이트 SQL과 함께 @Version 증가)
    }
}
