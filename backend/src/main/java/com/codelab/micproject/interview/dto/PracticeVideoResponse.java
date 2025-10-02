package com.codelab.micproject.interview.dto;

import com.codelab.micproject.interview.PracticeVideo;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PracticeVideoResponse {

    private Long id;
    private String videoUrl;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;

    public static PracticeVideoResponse from(PracticeVideo practiceVideo) {
        return new PracticeVideoResponse(
                practiceVideo.getId(),
                practiceVideo.getVideoUrl(),
                practiceVideo.getCreatedAt() != null ? practiceVideo.getCreatedAt() : LocalDateTime.now()
        );
    }
}