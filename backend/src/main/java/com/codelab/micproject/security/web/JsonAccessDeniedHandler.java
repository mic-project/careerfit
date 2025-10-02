package com.codelab.micproject.security.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** 인증은 있으나 권한 부족 → 403 JSON */
public class JsonAccessDeniedHandler implements AccessDeniedHandler {
    private static final ObjectMapper om = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Map.of(...) 대신 null 허용되는 HashMap 사용
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("status", 403);
        body.put("error", "Forbidden");
        body.put("message", ex != null && ex.getMessage() != null ? ex.getMessage()
                : "Forbidden. You don't have permission to access this resource.");
        body.put("path", request != null && request.getRequestURI() != null ? request.getRequestURI() : "");
        body.put("timestamp", Instant.now().toString());
        body.put("data", null); // HashMap은 null value 허용

        om.writeValue(response.getOutputStream(), body);
    }
}
