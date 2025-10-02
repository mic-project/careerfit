package com.codelab.micproject.resume.service;

import com.codelab.micproject.resume.config.GptProperties;
import com.codelab.micproject.resume.dto.ResumeOptions;
import com.codelab.micproject.resume.dto.SuggestionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Chat Completions 호출 클라이언트 (WebClient)
 * - GptProperties 를 통해 application.yml 의 gpt.* 설정을 주입받음
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GptClient {

    private final WebClient webClient = WebClient.builder().build();
    private final GptProperties props; // 설정 클래스 주입

    public String getModel() {
        return props.getModel();
    }

    /**
     * 이력서 본문을 요약 + 개선 포인트 제안으로 변환
     */
    @SuppressWarnings("unchecked")
    public AnalysisResult analyze(String text, ResumeOptions options) {
        String language = options != null ? options.getLanguage() : "ko";
        int suggestionCount = options != null ? options.getSuggestionCount() : 5;
        String tone = options != null ? options.getTone() : "formal";

        String system = """
                너는 이력서 컨설턴트다. 사용자가 제공한 이력서 텍스트를 기반으로,
                1) 핵심 역량 요약(5줄 이내)
                2) 개선 제안 %d개 (제목 + 1~2문장 설명)
                한국어(language=%s), 톤=%s 로 작성하라.
                출력은 JSON: {"summary":"...", "suggestions":[{"title":"...","detail":"..."}, ...]}
                """.formatted(suggestionCount, language, tone);

        Map<String, Object> body = Map.of(
                "model", props.getModel(),
                "temperature", props.getTemperature(),
                "max_tokens", props.getMaxTokens(),
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", text)
                )
        );

        Map<String, Object> resp = webClient.post()
                .uri(props.getApiUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String content = extractMessageContent(resp);
        ParsedJson pj = ParsedJson.parse(content);

        List<SuggestionItem> items = new ArrayList<>();
        for (ParsedJson.Item it : pj.items()) {
            items.add(SuggestionItem.builder().title(it.title()).detail(it.detail()).build());
        }

        return new AnalysisResult(pj.summary(), items);
    }

    // OpenAI 응답에서 message.content 추출
    private String extractMessageContent(Map<String, Object> resp) {
        try {
            var choices = (List<Map<String, Object>>) resp.get("choices");
            var message = (Map<String, Object>) choices.get(0).get("message");
            return String.valueOf(message.get("content"));
        } catch (Exception e) {
            log.error("OpenAI 응답 파싱 실패: {}", resp, e);
            return "{\"summary\":\"\",\"suggestions\":[]}";
        }
    }

    /** 분석 결과 DTO */
    public record AnalysisResult(String summary, List<SuggestionItem> suggestions){}

    /** 단순 JSON 파서 (운영환경에서는 Jackson ObjectMapper 사용 권장) */
    static class ParsedJson {
        record Item(String title, String detail){}
        private final String summary;
        private final List<Item> items;

        private ParsedJson(String summary, List<Item> items) {
            this.summary = summary; this.items = items;
        }

        public static ParsedJson parse(String s) {
            String sum = findValue(s, "\"summary\"");
            List<Item> items = new ArrayList<>();
            String arr = findArray(s, "\"suggestions\"");
            if (arr != null) {
                String[] parts = arr.split("\\},\\s*\\{");
                for (String p : parts) {
                    String t = findValue(p, "\"title\"");
                    String d = findValue(p, "\"detail\"");
                    if (t != null || d != null) items.add(new Item(nullToEmpty(t), nullToEmpty(d)));
                }
            }
            return new ParsedJson(nullToEmpty(sum), items);
        }

        private static String nullToEmpty(String v){ return v==null? "": v; }
        private static String findValue(String src, String key){
            int i = src.indexOf(key);
            if (i<0) return null;
            int q1 = src.indexOf('"', i + key.length());
            int q2 = src.indexOf('"', q1+1);
            return (q1>=0 && q2>q1) ? src.substring(q1+1, q2) : null;
        }
        private static String findArray(String src, String key){
            int i = src.indexOf(key);
            if (i<0) return null;
            int s = src.indexOf('[', i);
            int e = src.indexOf(']', s);
            return (s>=0 && e> s) ? src.substring(s+1, e) : null;
        }
        public String summary(){ return summary; }
        public List<Item> items(){ return items; }
    }
}