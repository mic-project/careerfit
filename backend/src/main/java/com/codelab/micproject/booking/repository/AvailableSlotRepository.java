// src/main/java/com/codelab/micproject/booking/repository/AvailableSlotRepository.java
package com.codelab.micproject.booking.repository;

import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.booking.domain.AvailableSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AvailableSlotRepository extends JpaRepository<AvailableSlot, Long> {
    List<AvailableSlot> findByConsultantAndStartAtBetweenOrderByStartAtAsc(
            User consultant, LocalDateTime from, LocalDateTime to
    );

    // ★ 추가: "정확히 같은 슬롯이 존재하는가?"
    boolean existsByConsultantAndStartAtAndEndAt(
            User consultant, LocalDateTime startAt, LocalDateTime endAt
    );

    // 디버깅용: 컨설턴트의 모든 슬롯 조회
    List<AvailableSlot> findByConsultant(User consultant);
}
