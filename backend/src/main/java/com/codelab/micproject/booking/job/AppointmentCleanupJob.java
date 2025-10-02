// com.codelab.micproject.booking.job.AppointmentCleanupJob
package com.codelab.micproject.booking.job;

import com.codelab.micproject.booking.domain.Appointment;
import com.codelab.micproject.booking.domain.AppointmentStatus;
import com.codelab.micproject.booking.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentCleanupJob {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final AppointmentRepository appointmentRepository;

    /**
     * 1분마다 스캔 → 15분 경과한 REQUESTED 를 CANCELLED 로 전환
     */
    @Transactional
    @Scheduled(fixedDelay = 60_000L)
    public void expireRequestedHolds() {
        LocalDateTime cutoff = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                .minusMinutes(15); // ← LocalDateTime
        List<Appointment> olds = appointmentRepository
                .findByStatusAndCreatedAtBefore(AppointmentStatus.REQUESTED, cutoff);

        olds.forEach(a -> a.setStatus(AppointmentStatus.CANCELLED));
        log.info("Expired {} pending appointments (REQUESTED -> CANCELLED)", olds.size());
    }
}
