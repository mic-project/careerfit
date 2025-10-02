package com.codelab.micproject.resume.controller;

import com.codelab.micproject.resume.dto.*;
import com.codelab.micproject.resume.service.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
@Validated
public class ResumeController {

    private final ResumeService resumeService;

    /**
     * 이력서 파일 업로드 + GPT 분석 요청
     * - Multipart 로 PDF/DOCX 업로드
     * - 옵션(언어/톤/포맷 등)을 함께 보낼 수 있음
     */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResumeAnalyzeResponse analyze(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart(value = "options", required = false) ResumeOptions options
    ) {
        ResumeAnalyzeRequest req = new ResumeAnalyzeRequest(file, options);
        return resumeService.analyze(req);
    }

    /**
     * 분석 결과 단건 조회 (id 기반)
     */
    @GetMapping("/{id}")
    public ResumeAnalyzeResponse getById(@PathVariable Long id) {
        return resumeService.getAnalysis(id);
    }
}
