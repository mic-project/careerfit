package com.codelab.micproject.payment.controller;

import com.codelab.micproject.common.response.ApiResponse;
import com.codelab.micproject.payment.dto.CheckoutRequest;
import com.codelab.micproject.payment.dto.CheckoutResponse;
import com.codelab.micproject.payment.dto.ConfirmRequest;
import com.codelab.micproject.payment.dto.PaymentWebhook;
import com.codelab.micproject.payment.service.PaymentService;
import com.codelab.micproject.security.oauth2.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService service;
    private final PaymentService paymentService;

    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN')")
    @PostMapping("/checkout")
    public ApiResponse<CheckoutResponse> checkout(@AuthenticationPrincipal UserPrincipal me,
                                                  @RequestBody @Valid CheckoutRequest req){
        return ApiResponse.ok(service.checkout(me, req));
    }

    /** PortOne 콜백 success 후 서버 검증 */
    @PostMapping("/confirm")
    public ApiResponse<Void> confirm(@RequestBody ConfirmRequest req){
        service.confirm(req);
        return ApiResponse.ok();
    }

    /** (선택) 시뮬 웹훅 */
    @PostMapping("/webhook")
    public ApiResponse<Void> webhook(@RequestBody @Valid PaymentWebhook webhook){
        paymentService.webhook(webhook);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN')")
    @PostMapping("/{orderId}/cancel")
    public ApiResponse<Void> cancel(@AuthenticationPrincipal UserPrincipal me,
                                    @PathVariable Long orderId,
                                    @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "USER_REQUEST") : "USER_REQUEST";
        paymentService.cancel(me, orderId, reason);
        return ApiResponse.ok();
    }
}
