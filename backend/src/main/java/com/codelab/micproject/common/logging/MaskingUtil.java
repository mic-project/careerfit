// com.codelab.micproject.common.logging.MaskingUtil
package com.codelab.micproject.common.logging;

public final class MaskingUtil {
    private MaskingUtil(){}

    public static String maskCardNumber(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("\\D", ""); // 숫자만
        if (digits.length() <= 4) return "****";
        return "****" + digits.substring(digits.length() - 4);
    }

    public static String maskCardBrand(String brand) {
        return brand; // 브랜드는 그대로 허용(원하면 일부 마스킹 가능)
    }
}
