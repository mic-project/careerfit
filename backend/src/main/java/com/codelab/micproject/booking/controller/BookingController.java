package com.codelab.micproject.booking.controller;

import com.codelab.micproject.booking.dto.AppointmentView;
import com.codelab.micproject.booking.dto.QuoteResponse;
import com.codelab.micproject.booking.dto.RequestAppointment;
import com.codelab.micproject.booking.dto.RequestAppointmentBatch;
import com.codelab.micproject.booking.service.BookingService;
import com.codelab.micproject.common.response.ApiResponse;
import com.codelab.micproject.security.oauth2.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/booking")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService service;

    /** 단일 슬롯 예약 요청 (REQUESTED 상태 생성) */
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN')")
    @PostMapping("/request")
    public ApiResponse<AppointmentView> request(
            @AuthenticationPrincipal UserPrincipal me,
            @RequestBody RequestAppointment req
    ) {
        return ApiResponse.ok(service.request(me, req));
    }

    /**
     * (유지) 배치 요청 엔드포인트.
     * 새 정책상 1회권만 허용하므로 슬롯 1개만 전달해야 하며,
     * 서비스에서 단일 요청으로 처리됩니다.
     */
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN')")
    @PostMapping("/request-batch")
    public ApiResponse<List<AppointmentView>> requestBatch(
            @AuthenticationPrincipal UserPrincipal me,
            @RequestBody RequestAppointmentBatch req
    ) {
        return ApiResponse.ok(service.requestBatch(me, req));
    }

    /** 컨설턴트 수동 승인(옵션). 결제 성공 시에는 자동 승인됨. */
    @PreAuthorize("hasAuthority('ROLE_CONSULTANT')")
    @PostMapping("/{id}/approve")
    public ApiResponse<AppointmentView> approve(
            @AuthenticationPrincipal UserPrincipal me,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(service.approve(me, id));
    }

    /** 결제 전 견적 미리보기: 등급별 1회권 가격만 반환 */
    @GetMapping("/quote")
    public ApiResponse<QuoteResponse> quote(@RequestParam Long consultantId) {
        return ApiResponse.ok(service.quote(consultantId));
    }
}
