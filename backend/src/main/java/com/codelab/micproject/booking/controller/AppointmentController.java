package com.codelab.micproject.booking.controller;

import com.codelab.micproject.booking.dto.RescheduleRequest;
import com.codelab.micproject.booking.service.AppointmentService;
import com.codelab.micproject.common.response.ApiResponse;
import com.codelab.micproject.security.oauth2.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    // 일정 변경 (본인 예약만)
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN','ROLE_CONSULTANT')")
    @PatchMapping("/{id}/reschedule")
    public ApiResponse<Void> reschedule(@PathVariable("id") Long appointmentId,
                                        @Valid @RequestBody RescheduleRequest body,
                                        @AuthenticationPrincipal UserPrincipal me) {
        appointmentService.reschedule(appointmentId, me, body);
        return ApiResponse.ok();
    }
}
