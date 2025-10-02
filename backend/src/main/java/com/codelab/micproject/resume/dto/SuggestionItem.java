package com.codelab.micproject.resume.dto;

import lombok.*;

/** 개선 제안 항목 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SuggestionItem {
    private String title;     // 제안 제목
    private String detail;    // 상세 설명
}
