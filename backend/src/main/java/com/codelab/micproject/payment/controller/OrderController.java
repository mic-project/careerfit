package com.codelab.micproject.payment.controller;

import com.codelab.micproject.common.response.ApiResponse;
import com.codelab.micproject.payment.service.OrderService;
import com.codelab.micproject.security.oauth2.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService service;

    public record CancelReq(String reason) {}

    // 사용자: 내 주문 취소 (결제 전/후 모두 허용. 후자는 환불기록 생성)
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN','ROLE_CONSULTANT')")
    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@AuthenticationPrincipal UserPrincipal me,
                                    @PathVariable Long id,
                                    @RequestBody(required = false) CancelReq req) {
        service.cancelMyOrder(me, id, req != null ? req.reason() : null);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('ROLE_CONSULTANT')")
    @PostMapping("/{id}/cancel-as-consultant")
    public ApiResponse<Void> cancelAsConsultant(@AuthenticationPrincipal UserPrincipal me,
                                                @PathVariable Long id,
                                                @RequestBody(required = false) CancelReq req) {
        service.cancelAsConsultant(me, id, req != null ? req.reason() : null);
        return ApiResponse.ok();
    }
}