// src/main/java/com/codelab/micproject/booking/domain/AvailableSlot.java
package com.codelab.micproject.booking.domain;

import com.codelab.micproject.account.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "available_slots",
        indexes = {
                @Index(name="idx_available_slot_consultant", columnList = "consultant_id"),
                @Index(name="idx_available_slot_range", columnList = "startAt,endAt")
        })
public class AvailableSlot {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User consultant;

    private LocalDateTime startAt;
    private LocalDateTime endAt;
}
