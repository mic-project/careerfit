package com.codelab.micproject.account.profile.controller;

import com.codelab.micproject.account.profile.dto.ProfileDto;
import com.codelab.micproject.account.profile.service.ProfileService;
import com.codelab.micproject.common.response.ApiResponse;
import com.codelab.micproject.security.oauth2.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController @RequestMapping("/api/profile") @RequiredArgsConstructor
public class ProfileController {
    private final ProfileService service;


    @GetMapping("/me")
    public ApiResponse<ProfileDto> my(@AuthenticationPrincipal UserPrincipal me){
        return ApiResponse.ok(service.my(me));
    }


    @PutMapping("/me")
    public ApiResponse<ProfileDto> upsert(@AuthenticationPrincipal UserPrincipal me, @RequestBody ProfileDto dto){
        return ApiResponse.ok(service.upsert(me, dto));
    }
}