package com.codelab.micproject.account.user.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserUpdateRequest {
    private String name;
    private String phone;
    private String password;        // 선택 (없으면 미변경)
    private String profileImage;    // URL로 직접 바꾸고 싶을 때만 사용(선택)
}