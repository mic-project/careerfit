package com.codelab.micproject.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 주민등록번호 등 민감정보를 비가역(hash) 저장할 때 사용할 수 있는 유틸 예시.
 * - 해시: SHA-256(salt + rrn + pepper)
 * - pepper는 환경변수/시크릿으로 운영
 */
public class SensitiveDataHasher {

    public static class HashResult {
        public final String saltB64;
        public final String hashB64;
        public HashResult(String saltB64, String hashB64) { this.saltB64 = saltB64; this.hashB64 = hashB64; }
    }

    public static HashResult hashWithPepper(String plain, String pepper) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            md.update(plain.getBytes(StandardCharsets.UTF_8));
            md.update(pepper.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            return new HashResult(Base64.getEncoder().encodeToString(salt),
                    Base64.getEncoder().encodeToString(digest));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
