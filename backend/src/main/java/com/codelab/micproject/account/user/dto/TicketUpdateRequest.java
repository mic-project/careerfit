package com.codelab.micproject.account.user.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketUpdateRequest {
    private Integer ticketCount;
    private LocalDate ticketStartDate;
    private LocalDate ticketEndDate;
}
