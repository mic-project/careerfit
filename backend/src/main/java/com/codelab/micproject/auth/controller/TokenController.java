//package com.codelab.micproject.auth.controller;
//
//import com.codelab.micproject.account.user.domain.User;
//import com.codelab.micproject.account.user.repository.UserRepository;
//import com.codelab.micproject.auth.domain.RefreshToken;
//import com.codelab.micproject.auth.service.RefreshTokenService;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.Duration;
//import java.time.Instant;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/auth")
//public class TokenController {
//
//    private final RefreshTokenService refreshTokenService;
//    private final JwtTokenProvider jwtTokenProvider;
//    private final UserRepository userRepository;
//    private final CookieSupport cookieSupport; // ★ 추가 (빈 주입)
//
//    public TokenController(RefreshTokenService refreshTokenService,
//                           JwtTokenProvider jwtTokenProvider,
//                           UserRepository userRepository,
//                           CookieSupport cookieSupport) { // ★ 추가
//        this.refreshTokenService = refreshTokenService;
//        this.jwtTokenProvider = jwtTokenProvider;
//        this.userRepository = userRepository;
//        this.cookieSupport = cookieSupport;     // ★ 추가
//    }
//
//    /** 로그인 성공 시 Refresh Token 발급 + HttpOnly 쿠키 저장 */
//    @PostMapping("/issue-refresh")
//    public ResponseEntity<?> issueRefresh(@RequestParam("userId") Long userId,
//                                          HttpServletResponse res) { // ★ 응답에 쿠키 심기용
//        RefreshToken rt = refreshTokenService.issue(userId, Duration.ofDays(14));
//        cookieSupport.setRefreshToken(rt.getToken(), (int) Duration.ofDays(14).toSeconds(), res); // ★ 인스턴스 메서드 사용
//        return ResponseEntity.ok(Map.of(
//                "message", "refresh issued",
//                "expiredAt", rt.getExpiresAt().toString()
//        ));
//    }
//
//    /** Access Token 재발급: refreshToken(쿠키) 검증 후 신규 Access 반환 */
//    @PostMapping("/refresh")
//    public ResponseEntity<?> refresh(@CookieValue(name = "refreshToken", required = false) String refreshCookie) {
//        if (refreshCookie == null) {
//            return ResponseEntity.status(401).body(Map.of("message", "refresh token cookie missing"));
//        }
//        RefreshToken rt = refreshTokenService.find(refreshCookie);
//        if (rt == null || rt.getExpiresAt().isBefore(Instant.now())) {
//            return ResponseEntity.status(401).body(Map.of("message", "refresh token invalid or expired"));
//        }
//
//        User user = userRepository.findById(rt.getUserId()).orElse(null);
//        if (user == null) {
//            return ResponseEntity.status(404).body(Map.of("message", "user not found"));
//        }
//
//        String newAccess = jwtTokenProvider.createAccessToken(user);
//        return ResponseEntity.ok(Map.of("accessToken", newAccess));
//    }
//}