// com.codelab.micproject.common.jpa.BaseTimeEntity
package com.codelab.micproject.common.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

// com.codelab.micproject.common.jpa.BaseTimeEntity
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;   // ← OffsetDateTime -> LocalDateTime

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;   // ← OffsetDateTime -> LocalDateTime

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

