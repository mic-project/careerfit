package com.codelab.micproject.controller.admin;

import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.account.user.domain.UserRole;
import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;

    // 이메일로 유저 검색
    @GetMapping("/search")
    public ApiResponse<User> searchUserByEmail(@RequestParam String email) {
        return userRepository.findByEmail(email)
                .map(ApiResponse::ok)
                .orElse(ApiResponse.error("유저를 찾을 수 없습니다."));
    }

    // 유저 권한 변경
    @PutMapping("/{id}/role")
    public ApiResponse<User> changeUserRole(
            @PathVariable Long id,
            @RequestParam UserRole role
    ) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setRole(role);
                    userRepository.save(user);
                    return ApiResponse.ok(user);
                })
                .orElse(ApiResponse.error("유저를 찾을 수 없습니다."));
    }
}
