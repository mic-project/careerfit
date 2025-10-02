package com.codelab.micproject.payment.controller;

import com.codelab.micproject.common.response.ApiResponse;
import com.codelab.micproject.payment.dto.OrderView;
import com.codelab.micproject.payment.service.MyPageService;
import com.codelab.micproject.security.oauth2.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {
    private final MyPageService service;

    // 사용자: 내 주문(예약 포함)
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN','ROLE_CONSULTANT')")
    @GetMapping("/orders")
    public ApiResponse<List<OrderView>> myOrders(@AuthenticationPrincipal UserPrincipal me) {
        return ApiResponse.ok(service.myOrders(me));
    }

    // 컨설턴트: 나에게 들어온 주문들
    @PreAuthorize("hasAuthority('ROLE_CONSULTANT')")
    @GetMapping("/consultant/orders")
    public ApiResponse<List<OrderView>> consultantOrders(@AuthenticationPrincipal UserPrincipal me) {
        return ApiResponse.ok(service.myConsultantOrders(me));
    }
}