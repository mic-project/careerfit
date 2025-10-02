package com.codelab.micproject.resume.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * GPT API 관련 설정(application.yml 의 gpt.* 바인딩)
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "gpt")
public class GptProperties {
    private String apiUrl;
    private String model = "gpt-4o-mini";
    private double temperature = 0.2;
    private int maxTokens = 800;
    private String apiKey = "";
}
