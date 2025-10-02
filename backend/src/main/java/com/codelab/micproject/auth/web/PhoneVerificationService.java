package com.codelab.micproject.auth.web;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 전화번호 인증 코드 뼈대 서비스.
 * - 실제 SMS 연동 대신 메모리에 6자리 코드를 저장/검증
 * - 코드 유효기간: 3분, 재요청 간격: 30초
 * - 서버 재시작 시 데이터 초기화 (운영에서는 Redis 등 외부 스토리지 추천)
 */
@Service
public class PhoneVerificationService {
    private static final Logger log = LoggerFactory.getLogger(PhoneVerificationService.class);

    private static final long EXPIRE_SEC = 180;   // 3분
    private static final long RESEND_GAP = 30;    // 30초
    private final Random random = new Random();

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public void sendCode(String phone) {
        Entry existing = store.get(phone);
        long now = Instant.now().getEpochSecond();
        if (existing != null && now - existing.getIssuedAt() < RESEND_GAP) {
            throw new IllegalStateException("코드 재요청은 잠시 후 가능합니다.");
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        store.put(phone, new Entry(code, now));
        // 실제 환경: 여기서 SMS 발송 API 연동 필요
        log.info("[PHONE_VERIFY] phone={} code={} (테스트용 로그, 운영 시 제거하세요)", phone, code);
    }

    public boolean verify(String phone, String code) {
        Entry e = store.get(phone);
        if (e == null) return false;
        long now = Instant.now().getEpochSecond();
        if (now - e.issuedAt > EXPIRE_SEC) {
            store.remove(phone);
            return false;
        }
        boolean ok = e.code.equals(code);
        if (ok) store.remove(phone); // 1회용
        return ok;
    }

    /** 현재 세션이 인증 완료 상태인지(선택적으로 회원가입시 활용) */
    public boolean isVerified(String phone) {
        // 간단히: 존재하지 않으면 인증 완료로 간주(엄격 적용하려면 verify 성공 시 별도 VerifiedSet에 저장)
        return !store.containsKey(phone);
    }

    @Getter
    private static class Entry {
        private final String code;
        private final long issuedAt;
        Entry(String code, long issuedAt) { this.code = code; this.issuedAt = issuedAt; }
    }
}
