package com.codelab.micproject.booking.repository;

import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.booking.domain.Appointment;
import com.codelab.micproject.booking.domain.AppointmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;   // ← 엔티티의 createdAt 타입에 맞춰 조정
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // 1) 리스케줄 동시성 제어
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.id = :id")
    Optional<Appointment> findByIdForUpdate(@Param("id") Long id);

    // 2) 겹침 검사 (생성/변경 공용) — 생성: excludeId=null, 변경: excludeId=대상ID
    @Query("""
        select (count(a) > 0) from Appointment a
         where a.consultant.id = :consultantId
           and (:excludeId is null or a.id <> :excludeId)
           and a.status in :blocking
           and a.startAt < :endAt
           and a.endAt   > :startAt
    """)
    boolean existsActiveOverlap(
            @Param("consultantId") Long consultantId,
            @Param("startAt")      OffsetDateTime startAt,
            @Param("endAt")        OffsetDateTime endAt,
            @Param("excludeId")    Long excludeId,
            @Param("blocking")     Collection<AppointmentStatus> blocking
    );

    // 3) 오래된 REQUESTED 자동 정리 (createdAt 타입 주의!)
    List<Appointment> findByStatusAndCreatedAtBefore(
            AppointmentStatus status,
            LocalDateTime cutoff
    );

    // 4) 편의 조회
    List<Appointment> findByUser(User u);
    List<Appointment> findByConsultant(User consultant);
    List<Appointment> findByConsultantAndStatus(User consultant, AppointmentStatus status);
}
