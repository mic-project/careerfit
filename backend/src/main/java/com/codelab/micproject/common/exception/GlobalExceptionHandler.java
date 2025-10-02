package com.codelab.micproject.common.exception;

import com.codelab.micproject.common.response.ApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> badCredential(BadCredentialsException e){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Void>> invalid(Exception e){
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid request: " + e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> illegal(IllegalStateException e){
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleExpired(ExpiredJwtException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("TOKEN_EXPIRED"));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleJwt(JwtException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("TOKEN_INVALID"));
    }

    // 🔚 딱 하나의 fallback만 유지
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAny(Exception e) {
        log.error("❌ [GlobalException] Unhandled exception: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> notFound(org.springframework.web.servlet.resource.NoResourceFoundException e){
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Not Found: " + e.getResourcePath()));
    }

    // 405: 지원하지 않는 HTTP 메서드
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handle405(HttpRequestMethodNotSupportedException ex) {
        String supported = ex.getSupportedHttpMethods() == null
                ? ""
                : ex.getSupportedHttpMethods().stream().map(HttpMethod::name).collect(Collectors.joining(", "));
        String msg = "Request method '" + ex.getMethod() + "' is not supported"
                + (supported.isEmpty() ? "" : ". Supported: " + supported);

        HttpHeaders headers = new HttpHeaders();
        if (ex.getSupportedHttpMethods() != null) headers.setAllow(ex.getSupportedHttpMethods());

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(headers)
                .body(ApiResponse.error("METHOD_NOT_ALLOWED" + msg));
    }

    // 400: 멀티파트/필수 파라미터 누락
    @ExceptionHandler({ MissingServletRequestParameterException.class, MissingServletRequestPartException.class })
    public ResponseEntity<ApiResponse<?>> handle400Param(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("BAD_REQUEST" + ex.getMessage()));
    }

    // 400: 멀티파트 파싱 실패
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<?>> handle400Multipart(MultipartException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("MULTIPART_ERROR" + ex.getMessage()));
    }

    // 413: 업로드 용량 초과
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<?>> handle413(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("PAYLOAD_TOO_LARGE" + "파일 용량 제한을 초과했습니다."));
    }
}

