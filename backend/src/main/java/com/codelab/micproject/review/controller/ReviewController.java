package com.codelab.micproject.review.controller;


import com.codelab.micproject.common.response.ApiResponse;
import com.codelab.micproject.review.dto.CreateReview;
import com.codelab.micproject.review.dto.ReviewView;
import com.codelab.micproject.review.service.ReviewService;
import com.codelab.micproject.security.oauth2.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService service;

    // 리뷰 작성: 로그인 필요
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_CONSULTANT','ROLE_ADMIN')")
    @PostMapping
    public ApiResponse<ReviewView> create(@AuthenticationPrincipal UserPrincipal me,
                                          @RequestBody @Valid CreateReview req){
        return ApiResponse.ok(service.create(me, req));
    }

    // 컨설턴트 리뷰 목록: 공개(예약 페이지에서 노출용)
    @PreAuthorize("permitAll()")
    @GetMapping("/consultant/{id}")
    public ApiResponse<List<ReviewView>> list(@PathVariable Long id){
        return ApiResponse.ok(service.listForConsultant(id));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_CONSULTANT','ROLE_ADMIN')")
    @GetMapping("/my/existence")
    public ApiResponse<Boolean> existsMine(@AuthenticationPrincipal UserPrincipal me,
                                           @RequestParam Long consultantId){
        return ApiResponse.ok(service.existsMyReviewForConsultant(me, consultantId));
    }
}