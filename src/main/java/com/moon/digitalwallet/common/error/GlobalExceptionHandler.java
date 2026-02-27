package com.moon.digitalwallet.common.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(ex.getErrorCode().name(), ex.getMessage(), requestId(req))
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(ErrorCode.INVALID_REQUEST.name(), "Validation failed", requestId(req)));
    }

    private String requestId(HttpServletRequest req) {
        return req.getHeader("X-Request-Id");
    }

}
