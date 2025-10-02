package com.codelab.micproject;

import com.codelab.micproject.resume.config.GptProperties;
import com.codelab.micproject.resume.config.ResumeModuleProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ResumeModuleProperties.class, GptProperties.class})
public class MicProjectApplication {
    public static void main(String[] args) {
        SpringApplication.run(MicProjectApplication.class, args);
    }
}