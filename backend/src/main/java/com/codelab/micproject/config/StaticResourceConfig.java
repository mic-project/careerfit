// src/main/java/com/codelab/micproject/config/StaticResourceConfig.java
package com.codelab.micproject.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    // LocalProfileImageStorage 와 동일해야 함
    @Value("${app.upload.profile-dir:uploads/profiles}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = "file:" + root.toString() + "/";

        // http://localhost:8080/uploads/xxx → <프로젝트>/uploads/profiles/xxx
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
