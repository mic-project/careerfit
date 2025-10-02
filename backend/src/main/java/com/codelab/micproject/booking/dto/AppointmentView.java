package com.codelab.micproject.booking.dto;

import com.codelab.micproject.booking.domain.Appointment;
import java.time.OffsetDateTime;

public record AppointmentView(
        Long id,
        Long consultantId,
        Long userId,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String status,
        String meetingUrl
) {
    public static AppointmentView from(Appointment a) {
        return new AppointmentView(
                a.getId(),
                a.getConsultant().getId(),
                a.getUser().getId(),
                a.getStartAt(),
                a.getEndAt(),
                a.getStatus().name(),   // Enum → String 변환
                a.getMeetingUrl()
        );
    }
}
