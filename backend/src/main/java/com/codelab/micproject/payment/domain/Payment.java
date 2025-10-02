// com.codelab.micproject.payment.domain.Payment
package com.codelab.micproject.payment.domain;

import jakarta.validation.constraints.Pattern;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Table(name="payments", uniqueConstraints = {
        @UniqueConstraint(name="uk_pay_merchant_uid", columnNames={"merchant_uid"})
})
public class Payment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // [중요] 우리 주문번호(멱등키)
    @Column(name="merchant_uid", nullable=false, unique=true)
    private String merchantUid;

    // 포트원 결제건 ID (웹훅/검증 후 채움)
    @Column(name="imp_uid")
    private String impUid;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status; // PENDING, PAID, FAILED, REFUNDED

    private String pgTransactionId;

    // 주문과 1:1 연결(권장) — 예약 확정/취소 시 묶음처리 쉬움
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", unique = true)
    private Order order;

    // (선택) 메서드 문자열 유지하면 편합니다 (CARD, KAKAO 등)
    private String method;

    private Long userId;
    private Long consultantId;
    private java.time.OffsetDateTime startAt;
    private java.time.OffsetDateTime endAt;

    @Column(precision=12, scale=0)
    private java.math.BigDecimal amount;

    private String currency;      // "KRW"
    private String pgProvider;    // "html5_inicis" 등
    private String payMethod;     // "card" 등
    private String receiptUrl;

    private Long appointmentId;   // 확정 후 연결한다면 사용

    @Column(name = "card_last4", length = 4)
    @Pattern(regexp = "^\\d{4}$", message = "cardLast4 must be 4 digits")
    private String cardLast4;

    @Column(name = "card_brand", length = 50)
    private String cardBrand;   // 예: "삼성카드"

    private java.time.OffsetDateTime paidAt;

    @Version
    private Long version;

    @org.hibernate.annotations.CreationTimestamp
    private java.time.OffsetDateTime createdAt;

    @org.hibernate.annotations.UpdateTimestamp
    private java.time.OffsetDateTime updatedAt;
}
