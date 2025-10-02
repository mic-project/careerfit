package com.codelab.micproject.resume.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/* 분석 요청 DTO (파일 + 옵션) */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResumeAnalyzeRequest {
    private MultipartFile file;
    private ResumeOptions options;
}
