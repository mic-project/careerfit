package com.codelab.micproject.common.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    // 저장하고 접근 가능한 URL 또는 경로(문자열) 반환
    String storeProfileImage(MultipartFile file, Long userId);
}
