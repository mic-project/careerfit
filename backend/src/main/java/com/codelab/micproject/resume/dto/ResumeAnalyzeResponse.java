package com.codelab.micproject.resume.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/** 분석 결과 응답 DTO */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ResumeAnalyzeResponse {
    private Long id;                      // DB 식별자
    private String requestId;             // 요청 추적용
    private String originalFilename;      // 업로드 파일명
    private String language;              // 분석 언어
    private String summary;               // 요약 결과(핵심 역량)
    private List<SuggestionItem> suggestions; // 개선 제안
    private String rawTextPreview;        // 파싱된 원문 일부(프리뷰)
    private LocalDateTime createdAt;
    private String model;                 // 사용 모델명
}
