package com.codelab.micproject.account.consultant.service;

import org.springframework.web.multipart.MultipartFile;

public interface ProfileImageStorage {
    String store(Long userId, MultipartFile file);
}
