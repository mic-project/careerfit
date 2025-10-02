package com.codelab.micproject.account.user.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "users")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true) // 소셜에서 이메일 미제공 시 임시 메일 생성 정책으로 충돌 방지 필요
    private String email;

    private String name;

    @Column(nullable = true)
    private String password;    // 휴대폰 번호

    @Column(nullable = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    // 소셜 제공자 고유키(예: kakao id)
    private String providerId;

    private String profileImage;

    @Builder.Default
    private boolean enabled = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 마이페이지용 이용권 관련 필드 추가
    private Integer ticketCount;         // 이용권 횟수 (예: 3회권)
    private LocalDate ticketStartDate;   // 이용권 시작일
    private LocalDate ticketEndDate;     // 이용권 종료일

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}
