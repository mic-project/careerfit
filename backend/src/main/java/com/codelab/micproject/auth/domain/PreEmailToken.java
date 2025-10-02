package com.codelab.micproject.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "pre_email_token", indexes = {
        @Index(name = "idx_pet_email", columnList = "email")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreEmailToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=255)
    private String email;

    @Column(nullable=false, length=100, unique = true)
    private String token;

    @Column(name="expires_at", nullable=false)
    private Instant expiresAt;

    @Column(nullable=false)
    private boolean verified;

    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}