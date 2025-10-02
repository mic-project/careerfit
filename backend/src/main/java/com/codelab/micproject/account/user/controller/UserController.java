package com.codelab.micproject.account.user.controller;

import com.codelab.micproject.account.user.dto.*;
import com.codelab.micproject.account.user.service.UserService;
import com.codelab.micproject.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /* 내 정보 조회 */
    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal(expression = "id") Long userId) {
        if (userId == null) return ApiResponse.ok(null);
        return ApiResponse.ok(userService.getMe(userId));
    }

    /* JSON PATCH (파일 없음) */
    @PatchMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<UserResponse> updateMeJson(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @RequestBody UserUpdateRequest request
    ) {
        if (userId == null) return ApiResponse.ok(null);
        return ApiResponse.ok(userService.updateMe(userId, request));
    }

    /* MULTIPART PATCH (파일 포함) */
    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<UserResponse> updateMeMultipart(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @RequestPart(required = false) String name,
            @RequestPart(required = false) String phone,
            @RequestPart(required = false) String password,
            @RequestPart(name = "profileImage", required = false) MultipartFile profileImage
    ) {
        if (userId == null) return ApiResponse.ok(null);
        return ApiResponse.ok(userService.updateMe(userId, name, phone, password, profileImage));
    }

    /* 이용권 수정 */
    @PatchMapping("/me/ticket")
    public ApiResponse<UserResponse> updateTicket(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @RequestBody TicketUpdateRequest request
    ) {
        if (userId == null) return ApiResponse.ok(null);
        return ApiResponse.ok(userService.updateTicket(userId, request));
    }

    /* 이용권 차감 */
    @PostMapping("/me/ticket/use")
    public ApiResponse<UserResponse> useTicket(@AuthenticationPrincipal(expression = "id") Long userId) {
        if (userId == null) return ApiResponse.ok(null);
        return ApiResponse.ok(userService.useTicket(userId));
    }

    // (티켓 로그 조회 메서드는 기존 그대로 두면 됩니다)
}
