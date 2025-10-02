package com.codelab.micproject.account.user.repository;

import com.codelab.micproject.account.user.domain.TicketUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketUsageLogRepository extends JpaRepository<TicketUsageLog, Long> {
    List<TicketUsageLog> findByUserIdOrderByUsedAtDesc(Long userId);
}
