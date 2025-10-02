package com.codelab.micproject.payment.support;


import com.codelab.micproject.account.consultant.domain.ConsultantLevel;

import java.util.Map;

public final class PricingPolicy {
    private PricingPolicy() {}

    // 원하면 yml로 뺄 수 있지만, 마감이므로 하드코딩으로 명확히
    private static final Map<ConsultantLevel, Integer> UNIT_PRICE = Map.of(
            ConsultantLevel.JUNIOR,    30000,
            ConsultantLevel.SENIOR,    50000,
            ConsultantLevel.EXECUTIVE, 80000
    );

    public static int unitPrice(ConsultantLevel level) {
        return UNIT_PRICE.getOrDefault(level, 50000); // default
    }
}
