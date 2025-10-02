package com.codelab.micproject.account.user.dto;

import com.codelab.micproject.account.user.domain.TicketUsageLog;

import java.time.LocalDateTime;

public record TicketUsageLogResponse(
        Long id,
        LocalDateTime usedAt,
        Integer remainingTickets
) {
    public static TicketUsageLogResponse from(TicketUsageLog log) {
        return new TicketUsageLogResponse(
                log.getId(),
                log.getUsedAt(),
                log.getRemainingTickets()
        );
    }
}
