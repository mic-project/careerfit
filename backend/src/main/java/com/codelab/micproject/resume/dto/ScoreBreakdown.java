package com.codelab.micproject.resume.dto;

import lombok.*;

/**
 * 각 항목별 점수 세부 breakdown DTO
 * - GPT 분석 결과에서 점수를 반환할 경우 사용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreBreakdown {

    /** 언어 사용 능력 점수 (0~100) */
    private int languageScore;

    /** 경력/프로젝트 경험의 적합성 점수 (0~100) */
    private int experienceScore;

    /** 기술 스택 매칭 점수 (0~100) */
    private int skillScore;

    /** 문서 구조 및 가독성 점수 (0~100) */
    private int structureScore;

    /** 전체 평균 점수 (자동 계산 또는 GPT 제공) */
    private int overallScore;
}
