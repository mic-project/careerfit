package com.codelab.micproject.auth.service;

import com.codelab.micproject.auth.domain.PreEmailToken;
import com.codelab.micproject.auth.repository.PreEmailTokenRepository;
import com.codelab.micproject.common.mail.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PreSignupEmailService {

    private final PreEmailTokenRepository preRepo;
    private final EmailService emailService;

    // ex) http://localhost:8080/api/auth/email/verify (EmailService에서도 사용하므로 남겨둠)
    @Value("${app.email.verify-base-url}")
    private String verifyBaseUrl;

    // 기본 발신 채널 설정 (GMAIL | NAVER). 수신 도메인으로 자동 분기하지 못할 때 사용
    @Value("${app.mail.default-provider:GMAIL}")
    private String defaultProvider;

    /** [1] 사전인증 토큰 발송 (토큰 발급 + 템플릿 메일 전송) */
    @Transactional
    public void send(String email) {
        // 이전 토큰 정리(선택)
        preRepo.deleteByEmail(email);

        // 토큰 발급 (하이픈 제거하면 URL/가독성 ↑)
        String token = UUID.randomUUID().toString().replace("-", "");

        // 저장
        PreEmailToken pet = PreEmailToken.builder()
                .email(email)
                .token(token)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .verified(false)
                .build();
        preRepo.save(pet);

        // 발신 채널 선택: naver.com → NAVER, gmail.com → GMAIL, 그 외는 기본값
        EmailService.Provider provider = pickProvider(email);

        // ✅ 예쁜 템플릿 메일 전송 (displayName은 아직 없으면 null)
        emailService.sendVerificationEmailStyled(provider, email, null, token);
    }

    /** [2] 메일 링크 검증 → verified=true 마킹 후 이메일 반환 */
    @Transactional
    public String verify(String token) {
        PreEmailToken pet = preRepo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        if (pet.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("만료된 토큰입니다.");
        }
        if (!pet.isVerified()) {
            pet.setVerified(true);
            preRepo.save(pet);
        }
        return pet.getEmail();
    }

    /** [3] 해당 이메일이 사전인증 완료 상태인지 */
    @Transactional(readOnly = true)
    public boolean isVerified(String email) {
        // 필요시 만료 시간도 함께 체크하려면 repo 메서드를 변경하세요.
        return preRepo.findTopByEmailOrderByIdDesc(email)
                .map(PreEmailToken::isVerified)
                .orElse(false);
    }

    /** [4] 가입 완료 후 사전 토큰 정리(선택) */
    @Transactional
    public void cleanup(String email) {
        preRepo.deleteByEmail(email);
    }

    // -------------------- 내부 유틸 --------------------
    private EmailService.Provider pickProvider(String toEmail) {
        String e = toEmail == null ? "" : toEmail.toLowerCase();
        if (e.endsWith("@naver.com")) return EmailService.Provider.NAVER;
        if (e.endsWith("@gmail.com")) return EmailService.Provider.GMAIL;
        return "NAVER".equalsIgnoreCase(defaultProvider)
                ? EmailService.Provider.NAVER
                : EmailService.Provider.GMAIL;
    }
}
