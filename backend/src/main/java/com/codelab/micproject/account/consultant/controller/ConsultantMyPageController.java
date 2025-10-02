package com.codelab.micproject.account.consultant.controller;

import com.codelab.micproject.account.consultant.dto.ConsultantMyPageDto;
import com.codelab.micproject.account.consultant.dto.UpdateConsultantForm;
import com.codelab.micproject.account.consultant.service.ConsultantMyPageService;
import com.codelab.micproject.booking.dto.AppointmentView;
import com.codelab.micproject.common.response.ApiResponse;
import com.codelab.micproject.security.oauth2.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/consultants/me")
@RequiredArgsConstructor
public class ConsultantMyPageController {

    private final ConsultantMyPageService service;

    @GetMapping
    public ApiResponse<ConsultantMyPageDto> my(@AuthenticationPrincipal UserPrincipal me){
        return ApiResponse.ok(service.getMyPage(me));
    }

    // multipart/form-data 바인딩을 위해 @ModelAttribute로 UpdateConsultantForm을 받음
    @PreAuthorize("hasAuthority('ROLE_CONSULTANT')")
    @PutMapping(consumes = {"multipart/form-data"})
    public ApiResponse<ConsultantMyPageDto> update(@AuthenticationPrincipal UserPrincipal me,
                                                   @ModelAttribute UpdateConsultantForm form){
        return ApiResponse.ok(service.updateMyPage(me, form));
    }

    @PreAuthorize("hasAuthority('ROLE_CONSULTANT')")
    @GetMapping("/schedules")
    public ApiResponse<List<AppointmentView>> schedules(@AuthenticationPrincipal UserPrincipal me){
        return ApiResponse.ok(service.getSchedules(me));
    }

    @PreAuthorize("hasAuthority('ROLE_CONSULTANT')")
    @GetMapping("/interviews/completed")
    public ApiResponse<List<AppointmentView>> completed(@AuthenticationPrincipal UserPrincipal me){
        return ApiResponse.ok(service.getCompletedInterviews(me));
    }
}
