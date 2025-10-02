package com.codelab.micproject.payment.portone;

import com.codelab.micproject.common.logging.MaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortOneClient {

    private final WebClient portone = WebClient.builder()
            .baseUrl("https://api.iamport.kr")
            .build();

    @Value("${portone.api-key}")    // application.properties: portone.api-key
    private String apiKey;

    @Value("${portone.api-secret}") // application.properties: portone.api-secret
    private String apiSecret;

    /** 액세스 토큰 */
    public String getAccessToken() {
        if (isBlank(apiKey) || isBlank(apiSecret)) {
            log.error("PortOne credentials are empty. apiKey? {}, apiSecret? {}", apiKey != null, apiSecret != null);
            throw new IllegalStateException("PORTONE_TOKEN_MISCONFIGURED");
        }

        Map<String, String> body = Map.of(
                "imp_key", apiKey.trim(),
                "imp_secret", apiSecret.trim()
        );

        Map<String, Object> outer = portone.post()
                .uri("/users/getToken")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(e -> {
                    log.error("PortOne getAccessToken HTTP error", e);
                    return Mono.error(new IllegalStateException("PORTONE_TOKEN_ERROR"));
                })
                .block();

        if (outer == null) throw new IllegalStateException("PORTONE_TOKEN_ERROR");
        Object code = outer.get("code");
        if (code instanceof Number && ((Number) code).intValue() != 0) {
            log.error("PortOne getAccessToken bad response: {}", outer);
            throw new IllegalStateException("PORTONE_TOKEN_ERROR");
        }
        Map<?, ?> resp = (Map<?, ?>) outer.get("response");
        if (resp == null || resp.get("access_token") == null) {
            log.error("PortOne getAccessToken missing token: {}", outer);
            throw new IllegalStateException("PORTONE_TOKEN_ERROR");
        }
        return String.valueOf(resp.get("access_token"));
    }

    /** imp_uid 단건 조회 */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPaymentByImpUid(String token, String impUid) {
        Map<String, Object> outer = portone.get()
                .uri("/payments/{impUid}", impUid)
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(e -> {
                    log.error("PortOne getPaymentByImpUid failed: impUid={}", impUid, e);
                    return Mono.error(new IllegalStateException("PORTONE_PAYMENT_QUERY_ERROR"));
                })
                .block();

        if (outer == null) throw new IllegalStateException("PORTONE_PAYMENT_QUERY_ERROR");
        Map<String, Object> response = (Map<String, Object>) outer.get("response");
        if (response == null) throw new IllegalStateException("PORTONE_PAYMENT_QUERY_ERROR");

        String cardName = (String) response.get("card_name");
        String cardNumber = (String) response.get("card_number");
        String last4 = MaskingUtil.maskCardNumber(cardNumber);
        String status = (String) response.get("status");
        Number amount = (Number) response.get("amount");
        log.info("PortOne payment fetched: impUid={}, status={}, amount={}, brand={}, last4={}",
                impUid, status, amount, cardName, last4);
        return response;
    }

    /** 결제 취소(환불) */
    @SuppressWarnings("unchecked")
    public Map<String, Object> cancelPayment(String token,
                                             String impUid,
                                             String merchantUid,
                                             BigDecimal amount,
                                             String reason) {

        var body = new java.util.HashMap<String, Object>();
        if (impUid != null) body.put("imp_uid", impUid);
        if (merchantUid != null) body.put("merchant_uid", merchantUid);
        if (amount != null) body.put("amount", amount.intValue()); // 원 단위 정수
        if (reason != null) body.put("reason", reason);

        Map<String, Object> outer = portone.post()
                .uri("/payments/cancel")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(e -> {
                    log.error("PortOne cancelPayment failed: impUid={}, merchantUid={}", impUid, merchantUid, e);
                    return Mono.error(new IllegalStateException("PORTONE_CANCEL_ERROR"));
                })
                .block();

        if (outer == null) throw new IllegalStateException("PORTONE_CANCEL_ERROR");
        Object code = outer.get("code");
        if (code instanceof Number && ((Number) code).intValue() != 0) {
            log.error("PortOne cancelPayment bad response: {}", outer);
            throw new IllegalStateException("PORTONE_CANCEL_ERROR");
        }
        return (Map<String, Object>) outer.get("response");
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
