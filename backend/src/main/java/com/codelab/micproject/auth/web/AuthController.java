package com.codelab.micproject.auth.web;

import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.account.user.dto.SignupRequest;
import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.account.user.service.AuthService;
import com.codelab.micproject.account.user.service.EmailVerificationService;
import com.codelab.micproject.auth.dto.UserProfileDto;
import com.codelab.micproject.common.util.CookieUtils;
import com.codelab.micproject.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    @Value("${app.cookie.domain:}")
    private String cookieDomain;
    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;
    @Value("${app.cookie.same-site:Lax}")
    private String sameSite;

//    /** 회원가입 */
//    @PostMapping("/signup")
//    public ResponseEntity<?> signup(@RequestBody SignupRequest req) {
//        authService.signup(req);
//        return ResponseEntity.ok("가입 완료! 이메일로 전송된 인증 링크를 확인하세요.");
//    }
//
//    /** 이메일 인증(메일 링크 클릭): GET /api/auth/email/verify?token=... */
//    @GetMapping("/email/verify")
//    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
//        emailVerificationService.verify(token);
//        return ResponseEntity.ok("이메일 인증이 완료되었습니다. 이제 로그인할 수 있습니다.");
//    }
//
//    /** 인증 메일 재전송: POST /api/auth/email/send  { "email": "..." } */
//    @PostMapping("/email/send")
//    public ResponseEntity<?> resend(@RequestBody Map<String, String> body) {
//        String email = body.get("email");
//        emailVerificationService.resend(email);
//        return ResponseEntity.ok("인증 메일을 다시 보냈습니다.");
//    }

    /** 현재 사용자 조회 (쿠키/헤더의 AccessToken 기준) */
    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest req, HttpServletResponse res) {
        String token = CookieUtils.getCookieValue(req, "ACCESS_TOKEN").orElse(null);
        if (token == null) return ResponseEntity.ok().body(null);

        try {
            Long userId = tokenProvider.getUserId(token);
            var u = userRepository.findById(userId).orElse(null);
            if (u == null) return ResponseEntity.ok().body(null);

            // ✅ DTO 변환
            UserProfileDto dto = UserProfileDto.builder()
                    .id(u.getId())
                    .email(u.getEmail())
                    .name(u.getName())
                    .profileImage(u.getProfileImage())
                    .role(u.getRole())
                    .provider(u.getProvider())
                    .build();

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            CookieUtils.expire(res, "ACCESS_TOKEN", "", false, "Lax");
            return ResponseEntity.ok().body(null);
        }
    }

//    /** 로그아웃: 쿠키 삭제 */
//    @PostMapping("/logout")
//    public ResponseEntity<?> logout(HttpServletResponse res) {
//        CookieUtils.deleteCookie(res, "ACCESS_TOKEN", cookieDomain, cookieSecure, sameSite);
//        CookieUtils.deleteCookie(res, "REFRESH_TOKEN", cookieDomain, cookieSecure, sameSite);
//        return ResponseEntity.ok().build();
//    }
}
