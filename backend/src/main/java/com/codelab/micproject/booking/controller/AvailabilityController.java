// src/main/java/com/codelab/micproject/booking/controller/AvailabilityController.java
package com.codelab.micproject.booking.controller;

import com.codelab.micproject.booking.dto.*;
import com.codelab.micproject.booking.service.AvailabilityService;
import com.codelab.micproject.common.response.ApiResponse;
import com.codelab.micproject.security.oauth2.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilityController {
    private final AvailabilityService service;

    // ===== 기존 유지 =====
    @PreAuthorize("hasAuthority('ROLE_CONSULTANT')")
    @PostMapping
    public ApiResponse<AvailabilityView> upsert(@AuthenticationPrincipal UserPrincipal me,
                                                @RequestBody @Valid UpsertAvailabilityReq req){
        return ApiResponse.ok(service.upsert(me, req));
    }

    @GetMapping("/{consultantId}/slots")
    public ApiResponse<List<SlotDto>> slots(@PathVariable Long consultantId,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to){
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("`to` must be the same or after `from`");
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        if (days > 60) {
            throw new IllegalArgumentException("Date range is too wide (max 60 days)");
        }
        return ApiResponse.ok(service.listSavedSlotsPublic(consultantId, from, to));
    }

    // ===== 프론트 요구 추가 (me/slots) =====
    @PreAuthorize("hasAuthority('ROLE_CONSULTANT')")
    @GetMapping("/me/slots")
    public ApiResponse<List<AvailabilitySlotDto>> mySlots(@AuthenticationPrincipal UserPrincipal me,
                                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(service.list(me, from, to));
    }

    @PreAuthorize("hasAuthority('ROLE_CONSULTANT')")
    @PostMapping("/me/slots")
    public ApiResponse<Void> addMySlots(@AuthenticationPrincipal UserPrincipal me,
                                        @RequestBody List<AvailabilitySlotDto> slots) {
        service.save(me, slots);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('ROLE_CONSULTANT')")
    @DeleteMapping("/me/slots/{id}")
    public ApiResponse<Void> deleteMySlot(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        service.delete(me, id);
        return ApiResponse.ok();
    }
}
