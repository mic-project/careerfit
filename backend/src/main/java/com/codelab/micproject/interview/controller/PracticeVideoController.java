package com.codelab.micproject.interview.controller;

import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.common.response.ApiResponse;
import com.codelab.micproject.interview.dto.PracticeVideoResponse;
import com.codelab.micproject.interview.service.PracticeVideoService;
import com.codelab.micproject.security.oauth2.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/practice-videos")
@RequiredArgsConstructor
public class PracticeVideoController {

    private final PracticeVideoService practiceVideoService;
    private final UserRepository userRepository;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<PracticeVideoResponse>>> getMyPracticeVideos(
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("🔍 /api/practice-videos/my 호출됨");
        log.info("🔍 principal: {}", principal);

        if (principal == null) {
            log.warn("⚠️ principal이 null입니다 - 인증되지 않은 요청");
            return ResponseEntity.ok(ApiResponse.error("로그인이 필요합니다."));
        }

        try {
            Long userId = principal.getId();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            List<PracticeVideoResponse> videos = practiceVideoService.getVideosByUser(user);
            log.info("✅ 사용자 {} 의 연습 영상 {} 개 조회 완료", userId, videos.size());

            return ResponseEntity.ok(ApiResponse.ok(videos));
        } catch (Exception e) {
            log.error("❌ 연습 영상 조회 실패", e);
            return ResponseEntity.ok(ApiResponse.error("영상 조회에 실패했습니다."));
        }
    }

    @DeleteMapping("/{videoId}")
    public ResponseEntity<ApiResponse<Void>> deletePracticeVideo(
            @PathVariable Long videoId,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("🗑️ /api/practice-videos/{} 삭제 요청", videoId);

        if (principal == null) {
            log.warn("⚠️ principal이 null입니다 - 인증되지 않은 요청");
            return ResponseEntity.ok(ApiResponse.error("로그인이 필요합니다."));
        }

        try {
            Long userId = principal.getId();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            practiceVideoService.deleteVideo(videoId, user);
            log.info("✅ 영상 {} 삭제 완료", videoId);

            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("❌ 영상 삭제 실패: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ 영상 삭제 중 오류 발생", e);
            return ResponseEntity.ok(ApiResponse.error("영상 삭제에 실패했습니다."));
        }
    }
}