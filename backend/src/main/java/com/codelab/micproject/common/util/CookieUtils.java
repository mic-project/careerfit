// common/util/CookieUtils.java
package com.codelab.micproject.common.util;

import jakarta.servlet.http.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Optional;

public class CookieUtils {

    // === 읽기 ===
    public static Optional<String> getCookieValue(HttpServletRequest req, String name) {
        if (req.getCookies() == null) return Optional.empty();
        for (Cookie c : req.getCookies()) {
            if (Objects.equals(c.getName(), name)) {
                return Optional.ofNullable(c.getValue());
            }
        }
        return Optional.empty();
    }

    public static Optional<Cookie> getCookie(HttpServletRequest req, String name) {
        if (req.getCookies() == null) return Optional.empty();
        for (Cookie c : req.getCookies()) {
            if (Objects.equals(c.getName(), name)) return Optional.of(c);
        }
        return Optional.empty();
    }

    public static void addHttpOnlyCookie(HttpServletResponse res, String name, String value,
                                         int maxAge, String domain, boolean secure, String sameSite) {

        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(maxAge);

        if (StringUtils.hasText(domain)) b.domain(domain);
        if (StringUtils.hasText(sameSite)) b.sameSite(sameSite); // Spring 6 가능

        res.addHeader(HttpHeaders.SET_COOKIE, b.build().toString());
    }

    public static void expire(HttpServletResponse res, String name,
                              String domain, boolean secure, String sameSite) {

        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(0);

        if (StringUtils.hasText(domain)) b.domain(domain);
        if (StringUtils.hasText(sameSite)) b.sameSite(sameSite);

        res.addHeader(HttpHeaders.SET_COOKIE, b.build().toString());
    }
}
