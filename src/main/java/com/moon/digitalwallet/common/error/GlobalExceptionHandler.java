package com.moon.digitalwallet.common.error;

import com.moon.digitalwallet.common.filter.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest req) {
        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(new ApiErrorResponse(ex.getErrorCode().name(), ex.getMessage(), requestId(req)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(HttpServletRequest req) {
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getHttpStatus())
                .body(new ApiErrorResponse(ErrorCode.INVALID_REQUEST.name(), "Validation failed", requestId(req)));
    }

    private String requestId(HttpServletRequest req) {
        Object requestId = req.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        if (requestId instanceof String id && !id.isBlank()) {
            return id;
        }
        return req.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
    }

}
