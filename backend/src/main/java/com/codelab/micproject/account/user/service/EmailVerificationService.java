package com.codelab.micproject.account.user.service;

import com.codelab.micproject.account.user.domain.EmailVerificationToken;
import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.account.user.repository.EmailVerificationTokenRepository;
import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.common.mail.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * 기본 Provider (도메인 매칭이 안 되거나 확장하고 싶을 때 사용)
     * - GMAIL / NAVER 중 선택. 기본값은 GMAIL
     */
    @Value("${app.mail.default-provider:GMAIL}")
    private EmailService.Provider defaultProvider;

    /**
     * 개발/테스트 용: 두 SMTP 모두로 전송
     * - 운영에서는 false 권장
     */
    @Value("${app.mail.double-send:false}")
    private boolean doubleSend;

    /** 회원가입 직후: 토큰 발급 + 메일 발송 */
    @Transactional
    public void issueAndSend(User user) {
        // 기존 미사용 토큰 무효화(선택)
        tokenRepository.invalidateAllForUser(user.getId());

        String token = UUID.randomUUID().toString();
        EmailVerificationToken entity = EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .used(false)
                .build();
        tokenRepository.save(entity);

        // 1) 도메인 기반 Provider 결정
        EmailService.Provider provider = resolveProvider(user.getEmail());
        log.info("[EmailVerify] provider={} email={}", provider, user.getEmail());

        // 2) 기본 전송
        emailService.sendVerificationEmail(provider, user.getEmail(), token);

        // 3) (옵션) 더블 전송 - 테스트용
        if (doubleSend) {
            EmailService.Provider other = (provider == EmailService.Provider.GMAIL)
                    ? EmailService.Provider.NAVER
                    : EmailService.Provider.GMAIL;
            log.info("[EmailVerify] double-send enabled. also sending via {}", other);
            emailService.sendVerificationEmail(other, user.getEmail(), token);
        }
    }

    /** 재전송: 이메일로 사용자 찾고 새 토큰 발급 + 발송 */
    @Transactional
    public void resend(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));
        if (user.isEnabled()) {
            throw new IllegalStateException("이미 이메일 인증이 완료된 계정입니다.");
        }
        issueAndSend(user);
    }

    /** 메일 링크 클릭 시 검증 */
    @Transactional
    public String verify(String token) {
        var evt = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰"));
        if (evt.isUsed()) throw new IllegalStateException("이미 사용된 토큰입니다.");
        if (evt.getExpiresAt().isBefore(Instant.now())) throw new IllegalStateException("만료된 토큰입니다.");

        User user = evt.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        evt.setUsed(true);
        tokenRepository.save(evt);        // or tokenRepository.delete(evt);  // 일회용이라면 삭제

        return user.getEmail();           // ← 컨트롤러에서 리다이렉트에 쓰기 좋게 이메일 반환
    }

    /** 이메일 도메인으로 Provider 결정 (필요 시 확장 가능) */
    private EmailService.Provider resolveProvider(String email) {
        String lower = email.toLowerCase(Locale.ROOT).trim();
        if (lower.endsWith("@naver.com")) {
            return EmailService.Provider.NAVER;
        }
        // 필요하면 도메인 추가:
        // else if (lower.endsWith("@gmail.com") || lower.endsWith("@googlemail.com")) { ... }
        return defaultProvider; // 기본은 GMAIL
    }
}
