package com.codelab.micproject.common.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path root = Paths.get("uploads/profile"); // 프로젝트 루트에 uploads/profile 폴더

    public LocalFileStorageService() throws IOException {
        Files.createDirectories(root);
    }

    @Override
    public String storeProfileImage(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) return null;
        try {
            String ext = getExtension(file.getOriginalFilename());
            String filename = "user-" + userId + "-" + System.currentTimeMillis() + (ext.isEmpty() ? "" : "." + ext);
            Path dest = root.resolve(filename);

            //같은 파일명이 있을 때 복사 실패를 피함
            // 기존 Files.copy(file.getInputStream(), dest);
            Files.copy(file.getInputStream(), dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // 로컬 테스트: 반환값을 프론트에서 /uploads/profile/... 로 맵핑해서 쓰거나, 서버에서 static resource로 서빙하세요.
            return "/uploads/profile/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패", e);
        }
    }

    private String getExtension(String name) {
        if (name == null) return "";
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1);
    }
}
