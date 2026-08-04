package com.regisx001.dQul.notification.exception;

import org.springframework.http.HttpStatus;

import com.regisx001.dQul.common.exception.BaseAppException;

public abstract class NotificationModuleException extends BaseAppException {

    private static final String MODULE_NAME = "NOTIFICATION";

    public NotificationModuleException(String message, HttpStatus status, String errorCode) {
        super(message, status, errorCode, MODULE_NAME);
    }

    public NotificationModuleException(String message, Throwable cause, HttpStatus status, String errorCode) {
        super(message, cause, status, errorCode, MODULE_NAME);
    }

    public NotificationModuleException(String message, HttpStatus status, String errorCode, Object details) {
        super(message, status, errorCode, MODULE_NAME, details);
    }
}
