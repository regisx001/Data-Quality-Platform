package com.regisx001.dQul.common.exception;

import org.springframework.http.HttpStatus;

public abstract class UserModuleException extends BaseAppException {

    private static final String MODULE_NAME = "USER";

    public UserModuleException(String message, HttpStatus status, String errorCode) {
        super(message, status, errorCode, MODULE_NAME);
    }

    public UserModuleException(String message, Throwable cause, HttpStatus status, String errorCode) {
        super(message, cause, status, errorCode, MODULE_NAME);
    }

    public UserModuleException(String message, HttpStatus status, String errorCode, Object details) {
        super(message, status, errorCode, MODULE_NAME, details);
    }
}
