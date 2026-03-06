package com.moon.digitalwallet.common.error;

@Deprecated
public class ApiException extends BusinessException {

    public ApiException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
