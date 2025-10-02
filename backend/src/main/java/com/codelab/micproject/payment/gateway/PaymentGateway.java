package com.codelab.micproject.payment.gateway;

public interface PaymentGateway {
    String issueMerchantUid(Long orderId);                 // merchant_uid 생성(우리 정책)
    String getAccessToken();                               // PortOne 토큰
    PortOnePaymentInfo getPaymentByImpUid(String impUid);  // 결제 단건 조회
    boolean verify(String impUid, String merchantUid, int expectedAmount);
}
