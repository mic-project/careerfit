package com.codelab.micproject.account.user.service;

import com.codelab.micproject.account.user.domain.AuthProvider;
import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.account.user.domain.UserRole;
import com.codelab.micproject.account.user.dto.LoginRequest;
import com.codelab.micproject.account.user.dto.SignupRequest;
import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtTokenProvider tokenProvider;

    // ✅ 추가: 이메일 인증 서비스
    private final EmailVerificationService emailVerificationService;

    /** 회원가입: 유저 저장(비활성) → 인증메일 발송 */
    @Transactional
    public void signup(SignupRequest req) {
        userRepository.findByEmail(req.email()).ifPresent(u -> {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        });

        User user = User.builder()
                .email(req.email())
                .password(encoder.encode(req.password()))
                .name(req.name())
                .role(UserRole.USER)
                .provider(AuthProvider.LOCAL)
                .enabled(true) // ★ 사전인증 완료이므로 true 로 저장
                .build();

        userRepository.save(user);

        // 기존: emailVerificationService.issueAndSend(user);  // ★ 제거
    }

    @Transactional(readOnly = true)
    public String login(LoginRequest req) {
        var user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new BadCredentialsException("잘못된 자격증명"));
        if (!encoder.matches(req.password(), user.getPassword())) {
            throw new BadCredentialsException("잘못된 자격증명");
        }
        if (!user.isEnabled()) {
            throw new BadCredentialsException("이메일 인증을 완료해 주세요.");
        }
        return tokenProvider.createAccessToken(user);
    }

    @Transactional
    public void signupImmediatelyEnabled(SignupRequest req) {
        userRepository.findByEmail(req.email()).ifPresent(u -> {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        });

        User user = User.builder()
                .email(req.email())
                .password(encoder.encode(req.password()))
                .name(req.name())
                .role(UserRole.USER)
                .provider(AuthProvider.LOCAL)
                .enabled(true) // 사전 인증 완료했으므로 바로 활성화
                .build();
        userRepository.save(user);
    }

    /** 소셜 신규 가입 (OAuth2 Success 후, SOCIAL_JOIN 기반 최종 생성) */
    @Transactional
    public User createSocialUser(Map<String, Object> payload, Map<String, String> body) {
        // payload: provider, providerId, email, name, image
        String providerStr = String.valueOf(payload.get("provider"));
        String providerId  = String.valueOf(payload.get("providerId"));
        String email       = (String) payload.get("email");
        String name        = (String) payload.get("name");
        String image       = (String) payload.get("image");

        AuthProvider provider = AuthProvider.valueOf(providerStr.toUpperCase());

        // 이미 존재하면 정책 선택 (여기선 존재 시 그대로 반환)
        User existed = userRepository.findByProviderAndProviderId(provider, providerId).orElse(null);
        if (existed != null) return existed;

        // body에서 추가 입력 받는 값 예: phone, nickName, agreeTos 등
        String phone    = body.getOrDefault("phone", null);
        String nickName = body.getOrDefault("nickName", null);

        // name 보정
        if (!StringUtils.hasText(name)) name = (nickName != null ? nickName : provider.name() + "_user");

        // 신규 생성
        User user = User.builder()
                .email(email != null ? email : provider.name().toLowerCase() + "-" + providerId + "@placeholder.local")
                .name(name)
                .password(null)                 // 소셜 로그인이라 패스워드 없음
                .role(UserRole.USER)
                .provider(provider)
                .providerId(providerId)
                .profileImage(StringUtils.hasText(image) ? image : null)
                .enabled(true)
                .build();

        // 필요하면 phone 저장하도록 엔티티/스키마에 필드 추가 후 set
        // user.setPhone(phone);

        return userRepository.save(user);
    }
}
