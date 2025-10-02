// security/oauth2/OAuth2AuthenticationFailureHandler.java
package com.codelab.micproject.security.oauth2;

import jakarta.servlet.http.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        res.sendRedirect("http://localhost:3000/social/failure?reason=" + ex.getMessage());
    }
}
