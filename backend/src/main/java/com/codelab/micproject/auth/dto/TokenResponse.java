// auth/dto/TokenResponse.java
package com.codelab.micproject.auth.dto;

import lombok.*;

@Getter @AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String refreshToken; // 쿠키 기반이면 응답 바디 생략 가능
    private String tokenType = "Bearer";
}
