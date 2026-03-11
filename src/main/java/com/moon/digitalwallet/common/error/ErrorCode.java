package com.moon.digitalwallet.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "account not found"),
    INSUFFICIENT_BALANCE(HttpStatus.UNPROCESSABLE_CONTENT, "insufficient balance"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "invalid request"),
    SAME_ACCOUNT_TRANSFER_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "same account transfer not allowed"),
    INVALID_TRANSFER_AMOUNT(HttpStatus.BAD_REQUEST, "invalid transfer amount"),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "concurrent modification"),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
