package com.codelab.micproject.payment.controller;

import com.codelab.micproject.common.response.ApiResponse;
import com.codelab.micproject.payment.dto.MyPaymentItem;
import com.codelab.micproject.payment.service.MyPaymentQueryService;
import com.codelab.micproject.security.oauth2.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPagePaymentController {
    private final MyPaymentQueryService query;

    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN')")
    @GetMapping("/payments")
    public ApiResponse<Page<MyPaymentItem>> myPayments(
            @AuthenticationPrincipal UserPrincipal me,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){
        return ApiResponse.ok(query.getMyPayments(me.id(), PageRequest.of(page, size)));
    }
}
