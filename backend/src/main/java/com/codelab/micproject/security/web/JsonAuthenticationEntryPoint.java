package com.codelab.micproject.security.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper om = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException ex) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType("application/json;charset=UTF-8");

        String message = (ex != null && ex.getMessage() != null)
                ? ex.getMessage()
                : "Unauthorized";
        String path = (request != null && request.getRequestURI() != null)
                ? request.getRequestURI()
                : "";

        Map<String, Object> body = new HashMap<>();
        body.put("status", 401);
        body.put("error", "Unauthorized");
        body.put("message", message);     // null 허용 X → 이미 방지
        body.put("path", path);           // null 허용 X → 이미 방지
        body.put("timestamp", Instant.now().toString());

        om.writeValue(response.getOutputStream(), body);
    }
}