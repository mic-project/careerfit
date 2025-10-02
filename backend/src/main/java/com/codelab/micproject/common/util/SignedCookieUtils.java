// common/util/SignedCookieUtils.java
package com.codelab.micproject.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

public class SignedCookieUtils {
    private static final ObjectMapper om = new ObjectMapper();

    public static void setSignedJson(HttpServletResponse res, String name, Map<String, Object> payload,
                                     int maxAgeSec, String domain, boolean secure, String sameSite, String secret) {
        try {
            String json = om.writeValueAsString(payload);
            String b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            String sig = hmacSha256(b64, secret);
            String value = b64 + "." + sig;

            ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(name, value)
                    .httpOnly(true).secure(secure).path("/").maxAge(maxAgeSec);
            if (domain != null && !domain.isBlank()) b.domain(domain);
            if (sameSite != null && !sameSite.isBlank()) b.sameSite(sameSite);

            res.addHeader(HttpHeaders.SET_COOKIE, b.build().toString());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static Map<String, Object> readSignedJson(String cookieValue, String secret) {
        try {
            String[] parts = cookieValue.split("\\.");
            if (parts.length != 2) return null;
            String payload = parts[0];
            String sig = parts[1];
            if (!hmacSha256(payload, secret).equals(sig)) return null;

            byte[] bytes = Base64.getUrlDecoder().decode(payload);
            return om.readValue(bytes, Map.class);
        } catch (Exception e) { return null; }
    }
    /** ✅ 오버로드: Request에서 쿠키를 찾아 바로 읽어준다. Optional로 감싸서 NPE 방지 */
    public static Optional<Map<String, Object>> readSignedJson(HttpServletRequest req, String name, String secret) {
        return CookieUtils.getCookieValue(req, name)
                .map(v -> readSignedJson(v, secret))
                .map(Optional::of)
                .orElse(Optional.empty());
    }
    private static String hmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }
}
