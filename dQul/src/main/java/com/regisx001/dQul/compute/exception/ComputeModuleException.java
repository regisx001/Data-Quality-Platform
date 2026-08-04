package com.regisx001.dQul.compute.exception;

import org.springframework.http.HttpStatus;

import com.regisx001.dQul.common.exception.BaseAppException;

public abstract class ComputeModuleException extends BaseAppException {

    private static final String MODULE_NAME = "COMPUTE";

    public ComputeModuleException(String message, HttpStatus status, String errorCode) {
        super(message, status, errorCode, MODULE_NAME);
    }

    public ComputeModuleException(String message, Throwable cause, HttpStatus status, String errorCode) {
        super(message, cause, status, errorCode, MODULE_NAME);
    }

    public ComputeModuleException(String message, HttpStatus status, String errorCode, Object details) {
        super(message, status, errorCode, MODULE_NAME, details);
    }
}
