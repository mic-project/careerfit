package com.codelab.micproject.payment.portone;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments/portone")
public class PortOneWebhookController {

    private final PortOneWebhookService service;

    // PortOne 콘솔에 등록하는 웹훅 엔드포인트
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody Map<String, Object> payload) {
        service.handle(payload);   // 멱등 처리 내부 보장
        return ResponseEntity.ok("OK");
    }
}
