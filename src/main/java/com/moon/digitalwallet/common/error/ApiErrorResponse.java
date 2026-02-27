package com.moon.digitalwallet.common.error;

public record ApiErrorResponse(String code, String message, String requestId) {
}
