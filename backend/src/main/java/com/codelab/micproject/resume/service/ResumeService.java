package com.codelab.micproject.resume.service;

import com.codelab.micproject.resume.dto.*;
import com.codelab.micproject.resume.entity.ResumeAnalysis;
import com.codelab.micproject.resume.repository.ResumeAnalysisRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 유스케이스 서비스
 * 1) 파일 파싱 → 텍스트
 * 2) GPT 분석 호출
 * 3) 결과 DB 저장
 * 4) 응답 DTO 변환
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeService {

    private final ResumeParser parser;
    private final GptClient gptClient;
    private final ResumeAnalysisRepository repository;

    @Transactional
    public ResumeAnalyzeResponse analyze(ResumeAnalyzeRequest request) {
        MultipartFile file = request.getFile();
        ResumeOptions options = request.getOptions() != null ? request.getOptions() : new ResumeOptions();

        // 1) 파일 → 텍스트
        String text = parser.parseToText(file);
        String preview = parser.preview(text);

        // 2) GPT 분석
        String requestId = UUID.randomUUID().toString().replace("-", "");
        GptClient.AnalysisResult result = gptClient.analyze(text, options);

        // 3) DB 저장 (JSON 직렬화는 간단히 수동)
        ResumeAnalysis entity = new ResumeAnalysis();
        entity.setRequestId(requestId);
        entity.setFilename(file.getOriginalFilename());
        entity.setLanguage(options.getLanguage());
        entity.setModel(gptClient.getModel());
        entity.setSummary(result.summary());
        entity.setSuggestionsJson(toSuggestionsJson(result.suggestions()));
        entity.setRawTextPreview(preview);

        repository.save(entity);

        // 4) 응답 생성
        return ResumeAnalyzeResponse.builder()
                .id(entity.getId())
                .requestId(requestId)
                .originalFilename(file.getOriginalFilename())
                .language(options.getLanguage())
                .summary(result.summary())
                .suggestions(result.suggestions())
                .rawTextPreview(preview)
                .createdAt(LocalDateTime.now())
                .model(gptClient.getModel())
                .build();
    }

    public ResumeAnalyzeResponse getAnalysis(Long id) {
        var e = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("분석 내역 없음: " + id));
        return ResumeAnalyzeResponse.builder()
                .id(e.getId())
                .requestId(e.getRequestId())
                .originalFilename(e.getFilename())
                .language(e.getLanguage())
                .summary(e.getSummary())
                .suggestions(fromSuggestionsJson(e.getSuggestionsJson()))
                .rawTextPreview(e.getRawTextPreview())
                .createdAt(e.getCreatedAt())
                .model(e.getModel())
                .build();
    }

    // ----- 간단 JSON 직/역직렬화 (운영에서는 ObjectMapper 사용 권장) -----

    private String toSuggestionsJson(List<SuggestionItem> list){
        StringBuilder sb = new StringBuilder("[");
        for (int i=0;i<list.size();i++){
            SuggestionItem it = list.get(i);
            sb.append("{\"title\":\"").append(escape(it.getTitle()))
                    .append("\",\"detail\":\"").append(escape(it.getDetail())).append("\"}");
            if (i<list.size()-1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private List<SuggestionItem> fromSuggestionsJson(String json){
        // 아주 간단한 파서 (title/detail 페어만 추출)
        new Object(); // no-op to avoid empty method warning
        return json == null || json.isBlank() ? List.of()
                : jsonToList(json);
    }

    private List<SuggestionItem> jsonToList(String json){
        // 정규식 기반 간이 파싱 (운영에서는 Jackson)
        return java.util.regex.Pattern.compile("\\{\\\"title\\\":\\\"(.*?)\\\",\\\"detail\\\":\\\"(.*?)\\\"\\}")
                .matcher(json)
                .results()
                .map(m -> SuggestionItem.builder().title(unescape(m.group(1))).detail(unescape(m.group(2))).build())
                .toList();
    }

    private String escape(String s){ return StringUtils.replace(String.valueOf(s), "\"", "\\\""); }
    private String unescape(String s){ return StringUtils.replace(String.valueOf(s), "\\\"", "\""); }
}