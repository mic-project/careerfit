package com.codelab.micproject.account.profile.domain;


import com.codelab.micproject.account.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;


@Entity
@Table(name = "profile")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Profile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;


    @Column(length = 1000)
    private String bio; // 소개


    private String skills; // 콤마 구분 태그 문자열 (간단 MVP)
    private String career; // 경력 요약
    private BigDecimal hourlyRate; // 시간당 요율(선택)
    @Builder.Default
    private boolean publicCalendar = true; // 캘린더 공개 여부
    // Profile.java 에 기존 필드들 아래에 추가
    private Integer careerStartYear; // ex: 2018 (null 허용)
}
