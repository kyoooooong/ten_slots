package com.tenslots.global.exception;

import com.tenslots.global.api.code.ErrorResultCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorResultCode errorCode;

    public BusinessException(ErrorResultCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorResultCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
