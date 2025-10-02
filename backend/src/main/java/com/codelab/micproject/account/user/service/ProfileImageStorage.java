package com.codelab.micproject.account.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class ProfileImageStorage {

    @Value("${app.upload.profile-dir:uploads/profiles}")
    private String profileDir;

    @Value("${app.static-base-url:http://localhost:8080}") // 리버스프록시/도메인 환경에 맞게
    private String staticBaseUrl;

    public String saveProfile(Long userId, MultipartFile file) {
        try {
            Files.createDirectories(Path.of(profileDir));
            String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String filename = userId + "_" + UUID.randomUUID() + (ext != null ? "." + ext : "");
            Path target = Path.of(profileDir, filename);
            file.transferTo(target.toFile());
            // 정적 리소스로 노출: /files/** 매핑(아래 Security/Static 설정 참고)
            return staticBaseUrl + "/files/profiles/" + filename;
        } catch (Exception e) {
            throw new RuntimeException("프로필 이미지 저장 실패", e);
        }
    }
}