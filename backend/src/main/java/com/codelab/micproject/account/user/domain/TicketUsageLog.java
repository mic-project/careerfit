package com.codelab.micproject.account.user.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class TicketUsageLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 사용자가 사용했는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 사용한 시각
    private LocalDateTime usedAt;

    // 사용 후 남은 횟수
    private Integer remainingTickets;

    @PrePersist
    void onCreate() {
        usedAt = LocalDateTime.now();
    }
}
