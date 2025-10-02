package com.codelab.micproject.resume.service;

import com.codelab.micproject.resume.config.ResumeModuleProperties;
import com.codelab.micproject.resume.exception.InvalidFileTypeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 파일 → 텍스트 파싱 역할
 * - PDF: PDFBox
 * - DOC/DOCX: Apache POI
 * - 안전을 위해 앞부분만 프리뷰로 저장(전량 저장은 보안/용량 이슈)
 */
@Component
@Slf4j
public class ResumeParser {

    private final ResumeModuleProperties props;

    public ResumeParser(ResumeModuleProperties props) {
        this.props = props;
    }

    public String parseToText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileTypeException("파일이 비어 있습니다.");
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

        // 일부 환경에서는 contentType 이 null/빈값/ octet-stream 으로 옴 → 확장자로 보정
        if (contentType == null || contentType.isBlank() || "application/octet-stream".equals(contentType)) {
            if (filename.endsWith(".txt")) contentType = "text/plain";
            else if (filename.endsWith(".md")) contentType = "text/markdown";
            else if (filename.endsWith(".pdf")) contentType = "application/pdf";
            else if (filename.endsWith(".doc")) contentType = "application/msword";
            else if (filename.endsWith(".docx")) contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }

        // 설정 기반 허용 타입 체크
        if (contentType == null || !props.getSupportedMimeTypes().contains(contentType)) {
            throw new InvalidFileTypeException("허용되지 않는 형식: " + contentType);
        }

        try {
            byte[] bytes = file.getBytes();

            // (선택) 크기 제한
            long mb = bytes.length / (1024 * 1024);
            if (mb > props.getMaxFileSizeMb()) {
                throw new InvalidFileTypeException("파일 용량 초과: " + mb + "MB");
            }

            return switch (contentType) {
                case "text/plain", "text/markdown" ->          // ✅ 추가
                        new String(bytes, StandardCharsets.UTF_8);
                case "application/pdf" ->
                        parsePdf(bytes);
                case "application/msword" ->
                        parseDoc(bytes);
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                        parseDocx(bytes);
                default -> throw new InvalidFileTypeException("파서 미구현 형식: " + contentType);
            };
        } catch (IOException e) {
            throw new RuntimeException("파일 파싱 실패", e);
        }
    }

    private String parsePdf(byte[] bytes) throws IOException {
        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(bytes))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }


    private String parseDoc(byte[] bytes) throws IOException {
        try (HWPFDocument doc = new HWPFDocument(new ByteArrayInputStream(bytes))) {
            try (WordExtractor extractor = new WordExtractor(doc)) {
                return extractor.getText();
            }
        }
    }

    private String parseDocx(byte[] bytes) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            try (XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                return extractor.getText();
            }
        }
    }

    /** 보안상 DB에는 프리뷰(앞 2,000자)만 저장 */
    public String preview(String fullText) {
        if (fullText == null) return "";
        byte[] raw = fullText.getBytes(StandardCharsets.UTF_8);
        int limit = Math.min(raw.length, 2000);
        return new String(raw, 0, limit, StandardCharsets.UTF_8);
    }
}
