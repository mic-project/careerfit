package com.codelab.micproject.auth.web;


import com.codelab.micproject.account.user.dto.LoginRequest;
import com.codelab.micproject.account.user.dto.SignupRequest;
import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.account.user.service.AuthService;
import com.codelab.micproject.auth.blacklist.TokenBlacklist;
import com.codelab.micproject.auth.domain.RefreshToken;
import com.codelab.micproject.auth.repository.RefreshTokenRepository;
import com.codelab.micproject.auth.service.PreSignupEmailService;
import com.codelab.micproject.common.util.CookieUtils;
import com.codelab.micproject.security.jwt.JwtTokenProvider;
import com.codelab.micproject.security.oauth2.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import com.codelab.micproject.common.response.ApiResponse;


import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class PasswordAuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PreSignupEmailService preSignupEmailService;
    private final TokenBlacklist blacklist;

    @Value("${app.cookie.domain:}")
    private String cookieDomain;
    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;
    @Value("${app.cookie.same-site:Lax}")
    private String sameSite;

    @Value("${app.jwt.secret}")             // ✅ (1) jwtSecret 주입
    private String jwtSecret;

    @Value("${app.frontend.verify-success-url:http://localhost:5173/FeatureAuthJoin}")
    private String verifySuccessUrl;

    // ───────────────── 이메일 사전 인증 플로우 ─────────────────

    /** [1] 이메일 입력 후 '인증' 버튼 → 사전 토큰 발송 */
    @PostMapping("/email/send")
    public ResponseEntity<?> sendEmail(@RequestBody EmailSendRequest req) {
        preSignupEmailService.send(req.email());
        return ResponseEntity.ok().build();
    }

    /** [2] 메일 링크 클릭 → 사전 토큰 검증 → 프론트로 302 리다이렉트 */
    @GetMapping("/email/verify")
    public ResponseEntity<Void> verifyEmail(@RequestParam @NotBlank String token) {
        String email = preSignupEmailService.verify(token);
        String redirect = UriComponentsBuilder
                .fromHttpUrl(verifySuccessUrl)
                .queryParam("verified", "1")
                .queryParam("email", email)
                .build(true)
                .toUriString();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", redirect)
                .build();
    }

    // ───────────────── 회원가입/로그인 ─────────────────

    /** [3] 회원가입(사전 인증 여부 체크) */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupForm form) {
        if (!form.password().equals(form.passwordConfirm())) {
            return ResponseEntity.badRequest().body("비밀번호 확인이 일치하지 않습니다.");
        }

        // 이메일 사전인증 필수
        if (!preSignupEmailService.isVerified(form.email())) {
            return ResponseEntity.badRequest().body("이메일 인증을 먼저 완료해 주세요.");
        }

        // 기존 시그니처로 위임 (enabled=false 생성을 원치 않으면 authService 수정 필요)
        SignupRequest req = new SignupRequest(
                form.email(),
                form.password(),
                form.name(),
                form.phone(),
                form.birthDate()
        );
        authService.signup(req); // 내부에서 enabled=false 저장 + (원래는 인증메일 발송) → 발송은 이제 필요 없음

        // 가입 완료 후 사전 토큰 정리(선택)
        preSignupEmailService.cleanup(form.email());

        return ResponseEntity.ok().build();
    }


    @GetMapping("/check-email")
    public ApiResponse<CheckEmailResponse> checkEmail(
            @RequestParam @Email @NotBlank String email
    ) {
        boolean exists = userRepository.existsByEmail(email);
        // available: 가입 안 되어 있으면 true
        // verified : 사전인증(또는 최종 인증) 여부. preSignupEmailService가 이런 메서드를 제공한다고 가정
        boolean verified = preSignupEmailService.isVerified(email); // 없으면 false로 고정해도 됩니다.

        return ApiResponse.ok(new CheckEmailResponse(!exists, verified));
    }

    public record CheckEmailResponse(boolean available, boolean verified) {}

    /** 로그인 */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginByEmail form, HttpServletResponse res) {

        // 0) 레거시/기존 쿠키를 먼저 만료시킴 (이름 혼재 정리)
        CookieUtils.expire(res, "ACCESS_TOKEN",  cookieDomain, cookieSecure, sameSite);
        CookieUtils.expire(res, "REFRESH_TOKEN", cookieDomain, cookieSecure, sameSite);

        // 1) 인증
        String accessToken = authService.login(new LoginRequest(form.email(), form.password()));
        Long userId = tokenProvider.getUserId(accessToken);

        // 2) RT 새로 발급 & DB 교체 저장
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime rtExpires = LocalDateTime.now().plusDays(tokenProvider.getRefreshExpDays());
        refreshTokenRepository.deleteByUserId(userId);
        refreshTokenRepository.save(new RefreshToken(refreshToken, userId, rtExpires));

        // 3) 쿠키 세팅 (발급/삭제와 동일 속성)
        int accessMaxAge  = (int) (tokenProvider.getAccessExpMin() * 60);
        int refreshMaxAge = (int) (tokenProvider.getRefreshExpDays() * 24 * 60 * 60);

        CookieUtils.addHttpOnlyCookie(res, "ACCESS_TOKEN",  accessToken,  accessMaxAge,  cookieDomain, cookieSecure, sameSite);
        CookieUtils.addHttpOnlyCookie(res, "REFRESH_TOKEN", refreshToken, refreshMaxAge, cookieDomain, cookieSecure, sameSite);

        return ResponseEntity.ok().build();
    }


    // ───────────────── DTO ─────────────────
    public static record SignupForm(
            @Email @NotBlank String email,
            @NotBlank String password,
            @NotBlank String passwordConfirm,
            @NotBlank String name,
            @Pattern(regexp = "^[0-9\\-]{9,15}$", message = "전화번호 형식") String phone,
            LocalDate birthDate
    ) {}

    public static record LoginByEmail(@Email @NotBlank String email, @NotBlank String password) {}
    public static record EmailSendRequest(@Email @NotBlank String email) {}

    // 공통 로직만 담당 (쿠키 만료, RT 삭제, AT 블랙리스트 등록)
    private void doLogout(HttpServletRequest req,
                          HttpServletResponse res,
                          Authentication authentication) {

        // 1) AT jti 블랙리스트 등록
        String raw = CookieUtils.getCookieValue(req, "ACCESS_TOKEN").orElse(null);
        if (raw != null) {
            String jti = tokenProvider.getJti(raw);
            if (jti != null) {
                long ttl = Math.max(0, tokenProvider.getExpiryAt(raw).getEpochSecond()
                        - java.time.Instant.now().getEpochSecond());
                blacklist.blacklist(jti, ttl);
            }
        }

        // 2) RT 하드 삭제
        Long userId = null;
        if (authentication instanceof UsernamePasswordAuthenticationToken t
                && t.getPrincipal() instanceof UserPrincipal p) {
            userId = p.getId();
        }
        if (userId != null) {
            refreshTokenRepository.deleteByUserId(userId);
        }

        // 3) 쿠키 만료 + 컨텍스트 정리
        CookieUtils.expire(res, "ACCESS_TOKEN",  cookieDomain, cookieSecure, sameSite);
        CookieUtils.expire(res, "REFRESH_TOKEN", cookieDomain, cookieSecure, sameSite);
        SecurityContextHolder.clearContext();
    }

    // ✅ 정식 엔드포인트(권장): 프론트가 XHR로 호출 → 204 응답 받고 화면 전환
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req,
                                       HttpServletResponse res,
                                       Authentication authentication) {
        log.info(">>> /api/auth/logout called");
        doLogout(req, res, authentication);
        return ResponseEntity.noContent().build(); // 204
    }

    // (선택) 편의용: 링크/주소창 접근을 위해 유지하고 싶다면 얇은 래퍼로만
    @GetMapping("/logout")
    public void logoutAndRedirect(HttpServletRequest req,
                                  HttpServletResponse res,
                                  Authentication authentication) throws IOException {
        doLogout(req, res, authentication);
        res.sendRedirect("http://localhost:5173/Login"); // 운영 도메인으로 교체
    }

}
