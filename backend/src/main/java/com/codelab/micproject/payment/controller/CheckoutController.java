// com.codelab.micproject.payment.controller.CheckoutController
package com.codelab.micproject.payment.controller;

import com.codelab.micproject.common.response.ApiResponse;
import com.codelab.micproject.payment.dto.CheckoutRequest;
import com.codelab.micproject.payment.dto.CheckoutResponse;
import com.codelab.micproject.payment.service.PaymentService;
import com.codelab.micproject.security.oauth2.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/booking")
@RequiredArgsConstructor
public class CheckoutController {
    private final PaymentService payments; // ✅ PaymentService로 위임

    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN')")
    @PostMapping("/checkout")
    public ApiResponse<CheckoutResponse> checkout(@AuthenticationPrincipal UserPrincipal me,
                                                  @RequestBody @Valid CheckoutRequest req) {
        return ApiResponse.ok(payments.checkout(me, req));
    }
}

