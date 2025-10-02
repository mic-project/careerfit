package com.codelab.micproject.interview.service;

import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.interview.InterviewPracticeVideoRepository;
import com.codelab.micproject.interview.PracticeVideo;
import com.codelab.micproject.interview.dto.PracticeVideoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PracticeVideoService {

    private final InterviewPracticeVideoRepository practiceVideoRepository;

    public List<PracticeVideoResponse> getVideosByUser(User user) {
        List<PracticeVideo> videos = practiceVideoRepository.findByUserOrderByCreatedAtDesc(user);
        return videos.stream()
                .map(PracticeVideoResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteVideo(Long videoId, User user) {
        PracticeVideo video = practiceVideoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("영상을 찾을 수 없습니다."));

        if (!video.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("본인의 영상만 삭제할 수 있습니다.");
        }

        practiceVideoRepository.delete(video);
    }
}