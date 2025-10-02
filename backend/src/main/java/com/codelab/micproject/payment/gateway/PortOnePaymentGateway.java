package com.codelab.micproject.payment.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

// payment/gateway/portone/PortOnePaymentGateway.java
@Component
@RequiredArgsConstructor
public class PortOnePaymentGateway implements PaymentGateway {
    @Value("${portone.api-key}")    private String apiKey;
    @Value("${portone.api-secret}") private String apiSecret;

    private final WebClient web = WebClient.builder()
            .baseUrl("https://api.iamport.kr").build();

    @Override
    public String issueMerchantUid(Long orderId) {
        // 운영 전환 시 규칙 유지: ex) CF-ORD-{orderId}-{yyyyMMddHHmmss}
        return "CF-ORD-" + orderId + "-" + System.currentTimeMillis();
    }

    @Override
    public String getAccessToken() {
        var res = web.post().uri("/users/getToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("imp_key", apiKey, "imp_secret", apiSecret))
                .retrieve().bodyToMono(JsonNode.class).block();
        return res.get("response").get("access_token").asText();
    }

    @Override
    public PortOnePaymentInfo getPaymentByImpUid(String impUid) {
        String token = getAccessToken();
        var res = web.get().uri("/payments/{impUid}", impUid)
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve().bodyToMono(JsonNode.class).block();
        var p = res.get("response");

        String cardNumber = p.path("card_number").asText(null); // "1234-****-****-4929"
        String last4 = null;
        if (cardNumber != null && cardNumber.length() >= 4) {
            last4 = cardNumber.substring(cardNumber.length() - 4);
        }
        String brand = p.path("card_name").asText(null);        // "삼성카드" 등
        long paidAtEpoch = p.path("paid_at").asLong(0);         // epoch seconds

        return new PortOnePaymentInfo(
                p.get("imp_uid").asText(),
                p.get("merchant_uid").asText(),
                p.get("status").asText(),       // paid, ready, failed...
                p.get("amount").asInt(),
                brand, last4, paidAtEpoch       // 🔹 여기를 PortOnePaymentInfo에 추가
        );
    }

    @Override
    public boolean verify(String impUid, String merchantUid, int expectedAmount) {
        var info = getPaymentByImpUid(impUid);
        return "paid".equals(info.status())
                && merchantUid.equals(info.merchantUid())
                && expectedAmount == info.amount();
    }
}



