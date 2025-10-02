package com.codelab.micproject.payment.domain;

import com.codelab.micproject.account.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Table(
        name = "orders", // ✅ 예약어 회피
        indexes = {
                @Index(name = "idx_orders_user_id", columnList = "user_id"),
                @Index(name = "idx_orders_consultant_id", columnList = "consultant_id")
        }
)
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultant_id", nullable = false)
    private User consultant;

    private int bundleCount; // 1/3/5

    @Column(precision = 12, scale = 0, nullable = false)
    private BigDecimal unitPrice; // 회당 가격

    @Column(precision = 12, scale = 0, nullable = false)
    private BigDecimal totalPrice; // = unitPrice * bundleCount

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderAppointment> appointments = new ArrayList<>();

    @Version
    private Long version;

    /** 생성 시각 기본값 */
    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
