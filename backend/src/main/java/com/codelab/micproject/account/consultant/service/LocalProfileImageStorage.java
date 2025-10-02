// src/main/java/com/codelab/micproject/account/consultant/service/LocalProfileImageStorage.java
package com.codelab.micproject.account.consultant.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LocalProfileImageStorage implements ProfileImageStorage {

    @Value("${app.upload.profile-dir:uploads/profiles}")
    private String baseDir;

    @Override
    public String store(Long userId, MultipartFile file) {
        try {
            String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String name = userId + "_" + UUID.randomUUID() + (ext != null ? "." + ext : "");
            Path dir = Path.of(baseDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(name);
            file.transferTo(target.toFile());
            // StaticResourceConfig: /uploads/** → uploads/profiles/**
            return "/uploads/" + name;
        } catch (Exception e) {
            throw new RuntimeException("프로필 이미지 저장 실패", e);
        }
    }
}
