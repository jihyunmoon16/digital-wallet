package com.moon.digitalwallet.common.error;

public enum ErrorCode {
    ACCOUNT_NOT_FOUND("account not found"),
    INSUFFICIENT_BALANCE("insufficient balance"),
    INVALID_REQUEST("invalid request");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
