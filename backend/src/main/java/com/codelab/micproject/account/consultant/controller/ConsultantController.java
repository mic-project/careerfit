package com.codelab.micproject.account.consultant.controller;


import com.codelab.micproject.account.consultant.dto.ConsultantCardDto;
import com.codelab.micproject.account.consultant.service.ConsultantService;

import com.codelab.micproject.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


import java.util.List;


@RestController @RequestMapping("/api/consultants") @RequiredArgsConstructor
public class ConsultantController {
    private final ConsultantService service;


    // 공개 리스트 (필터/정렬)
    @GetMapping
    public ApiResponse<List<ConsultantCardDto>> list(@RequestParam(required = false) String level,
                                                     @RequestParam(required = false) Double minRating,
                                                     @RequestParam(required = false, defaultValue = "rating") String sort){
        return ApiResponse.ok(service.list(level, minRating, sort));
    }
}