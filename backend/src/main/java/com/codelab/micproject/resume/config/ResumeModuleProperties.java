package com.codelab.micproject.resume.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * 이력서 모듈 관련 설정(application.yml 의 resume.* 바인딩)
 */
// ResumeModuleProperties.java
@ConfigurationProperties(prefix = "resume")
@Getter @Setter
public class ResumeModuleProperties {
    private List<String> supportedTypes = List.of("pdf","doc","docx","txt");
    private List<String> supportedMimeTypes = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain", // ✅ 추가
            "text/markdown"
    );
    private int maxFileSizeMb = 10;
}

