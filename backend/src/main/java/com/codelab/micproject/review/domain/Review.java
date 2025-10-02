package com.codelab.micproject.review.domain;


import com.codelab.micproject.account.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="reviewer_id")
    private User reviewer;


    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="consultant_id")
    private User consultant;


    private int rating; // 1~5
    @Column(length = 1000) private String comment;


    @CreationTimestamp
    private OffsetDateTime createdAt;

    @ElementCollection
    @CollectionTable(name = "review_tags", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "tag", length = 20)
    private List<String> tags = new ArrayList<>();
}