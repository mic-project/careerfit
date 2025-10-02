// config/CorsConfig.java (새 파일)
package com.codelab.micproject.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry r) {
                r.addMapping("/**")
                        .allowedOrigins("http://localhost:5173", "https://app.careerfit.net") // ★ production 도메인 추가
                        .allowedMethods("GET","POST","PUT","DELETE","PATCH","OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)                  // ★ 쿠키 허용
                        .maxAge(3600);
            }
        };
    }
}
