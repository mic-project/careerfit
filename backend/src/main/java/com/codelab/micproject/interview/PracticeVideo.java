package com.codelab.micproject.interview;

import com.codelab.micproject.account.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PracticeVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 1024)
    private String videoUrl;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public PracticeVideo(String videoUrl, User user) {
        this.videoUrl = videoUrl;
        this.user = user;
    }

    // User 없이 생성 (익명 사용자용)
    public PracticeVideo(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}
