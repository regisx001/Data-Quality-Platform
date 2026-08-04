package com.regisx001.dQul.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public abstract class BaseAppException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final String module;
    private final Object details;

    public BaseAppException(String message, HttpStatus status, String errorCode, String module) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.module = module;
        this.details = null;
    }

    public BaseAppException(String message, Throwable cause, HttpStatus status, String errorCode, String module) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
        this.module = module;
        this.details = null;
    }

    public BaseAppException(String message, HttpStatus status, String errorCode, String module, Object details) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.module = module;
        this.details = details;
    }

    public BaseAppException(String message, Throwable cause, HttpStatus status, String errorCode, String module, Object details) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
        this.module = module;
        this.details = details;
    }
}
