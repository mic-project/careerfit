package com.codelab.micproject.booking.domain;


import com.codelab.micproject.account.user.domain.User;
import jakarta.persistence.*;
import lombok.*;


@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(indexes = {@Index(columnList = "consultant_id, weekday")})
public class Availability {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="consultant_id")
    private User consultant; // ROLE_CONSULTANT


    /** ISO 요일 1~7 (Mon~Sun) */
    private int weekday;


    /** HH:mm (로컬 타임) */
    private String startTime;
    /** HH:mm (로컬 타임) */
    private String endTime;


    /** 분 단위 슬롯 크기 (예: 30) */
    private int slotMinutes;


    /** 타임존 ID (예: Asia/Seoul). null이면 시스템 기본 */
    private String zoneId;
}