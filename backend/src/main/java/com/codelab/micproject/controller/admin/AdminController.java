package com.codelab.micproject.controller.admin;


import com.codelab.micproject.account.user.domain.UserRole;
import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserRepository userRepository;


    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/users/{id}/role")
    public ApiResponse<Void> changeRole(@PathVariable Long id, @RequestParam UserRole role){
        var u = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("user not found"));
        u.setRole(role);
        userRepository.save(u);
        return ApiResponse.ok();
    }
}
