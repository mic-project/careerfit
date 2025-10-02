// auth/domain/RefreshToken.java
package com.codelab.micproject.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class RefreshToken {
    @Id
    private String token; // RT를 PK로 보관(문자열)

    private Long userId;

    private LocalDateTime expiresAt; // 만료시간 인덱스 고려(대량 청소 작업시)
}
