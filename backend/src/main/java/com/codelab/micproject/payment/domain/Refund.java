package com.codelab.micproject.payment.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(indexes = {@Index(columnList = "order_id")})
public class Refund {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id")
    private Order order;

    @Column(precision = 12, scale = 0)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private RefundStatus status;

    private String reason;

    private OffsetDateTime createdAt;
    private OffsetDateTime completedAt;
}