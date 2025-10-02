// security/jwt/JwtProperties.java
package com.codelab.micproject.security.jwt;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class JwtProperties {
    @Value("${app.jwt.issuer}")
    private String issuer;
    @Value("${app.jwt.secret}")
    private String secret;
    @Value("${app.jwt.access-exp-min}")
    private long accessExpMin;
    @Value("${app.jwt.refresh-exp-days}")
    private long refreshExpDays;
}
