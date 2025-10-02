package com.codelab.micproject.resume.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor // <- 'cannot find symbol' 오류 해결을 위해 추가
public class ResumeOptions {

    @Builder.Default
    private String language = "ko";

    @Builder.Default
    private String tone = "formal";

    @Builder.Default // <- 경고 해결을 위해 추가
    private int maxKeywords = 10;

    @Builder.Default
    private int suggestionCount = 5;
}
