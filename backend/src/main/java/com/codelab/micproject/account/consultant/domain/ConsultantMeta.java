package com.codelab.micproject.account.consultant.domain;


import com.codelab.micproject.account.user.domain.User;
import jakarta.persistence.*;
import lombok.*;


import java.math.BigDecimal;


@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(indexes = {@Index(name="idx_consultant_meta_consultant", columnList = "consultant_id", unique = true)})
public class ConsultantMeta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultant_id", unique = true)
    private User consultant;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ConsultantLevel level = ConsultantLevel.JUNIOR;


    // 기본 단가(회당). null이면 시스템 기본값 사용
    @Column(precision = 12, scale = 0)
    private BigDecimal basePrice;
}