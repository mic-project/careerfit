package com.codelab.micproject.common.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Qualifier("gmailSender")
    private final JavaMailSender gmailSender;

    @Qualifier("naverSender")
    private final JavaMailSender naverSender;

    @Value("${app.email.verify-base-url}")
    private String verifyBaseUrl;

    @Value("${app.mail.gmail.from}")
    private String gmailFrom;  // 보통 username과 동일
    @Value("${app.mail.naver.from}")
    private String naverFrom;  // 보통 username과 동일

    @Value("${app.mail.from.name:CareerFit}")
    private String fromName;   // 표시 이름(없으면 CareerFit)

    public enum Provider { GMAIL, NAVER }

    public void sendVerificationEmail(Provider provider, String to, String token) {
        String subject = "[CareerFit] 이메일 인증을 완료해 주세요";
        String verifyLink = verifyBaseUrl + "?token=" + token;
        String html = """
            <h3>이메일 인증</h3>
            <p>아래 링크를 눌러 인증을 완료해 주세요:</p>
            <a href="%s">%s</a>
        """.formatted(verifyLink, verifyLink);

        JavaMailSender sender = (provider == Provider.GMAIL) ? gmailSender : naverSender;
        String from = (provider == Provider.GMAIL) ? gmailFrom : naverFrom;
        sendHtml(sender, from, to, subject, html);
    }

    /** 예쁜 템플릿으로 인증메일 보내기 */
    public void sendVerificationEmailStyled(Provider provider, String to, String displayName, String token) {
        String subject = "[CareerFit] 이메일 인증을 완료해 주세요";
        String verifyLink = verifyBaseUrl + "?token=" + urlEncode(token);

        // 템플릿 로드 & 치환
        String html = loadTemplate("templates/email-verify.html")
                .replace("{{appName}}", "CareerFit")
                .replace("{{displayName}}", (displayName == null || displayName.isBlank()) ? "회원" : displayName)
                .replace("{{verifyUrl}}", verifyLink)
                .replace("{{logoUrl}}", "https://YOUR-CDN/logo/careerfit-white.png") // 절대경로로 변경
                .replace("{{year}}", String.valueOf(java.time.Year.now().getValue()));

        JavaMailSender sender = (provider == Provider.GMAIL) ? gmailSender : naverSender;
        String from = (provider == Provider.GMAIL) ? gmailFrom : naverFrom;
        sendHtml(sender, from, to, subject, html);
    }

    private void sendHtml(JavaMailSender sender, String from, String to, String subject, String html) {
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, /*multipart*/ true, "UTF-8");
            // From: SMTP 사용자와 동일 필요 (특히 NAVER)
            helper.setFrom(from, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            // 👍 로고 첨부 (CID = "careerfitLogo")
            org.springframework.core.io.ClassPathResource logo =
                    new org.springframework.core.io.ClassPathResource("templates/mail/careerfit-logo.png");
            helper.addInline("careerfitLogo", logo, "image/png");

            sender.send(message);
        } catch (Exception e) {
            System.err.println("Mail send failed: " + e.getMessage());
            throw new IllegalStateException("이메일 전송 실패", e);
        }
    }

    private String loadTemplate(String ignored) {
        String[] candidates = {
                "templates/email-verify.html",
                "email-verify.html",
                "mail/email-verify.html"
        };
        for (String path : candidates) {
            try (var is = new org.springframework.core.io.ClassPathResource(path).getInputStream()) {
                return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception ignore) { }
        }
        throw new IllegalStateException("메일 템플릿 로드 실패: templates/email-verify.html");
    }


    private String urlEncode(String v) {
        return java.net.URLEncoder.encode(v, java.nio.charset.StandardCharsets.UTF_8);
    }

    public void sendHtmlAutoFrom(String to, String subject, String html) {
        Provider provider = to.endsWith("@naver.com") ? Provider.NAVER : Provider.GMAIL;
        JavaMailSender sender = (provider == Provider.GMAIL) ? gmailSender : naverSender;
        String from = (provider == Provider.GMAIL) ? gmailFrom : naverFrom; // @Value로 주입
        sendHtml(sender, from, to, subject, html);
    }
}
