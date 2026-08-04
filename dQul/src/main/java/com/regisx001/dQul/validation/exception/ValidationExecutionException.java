package com.regisx001.dQul.validation.exception;

import org.springframework.http.HttpStatus;

public class ValidationExecutionException extends ValidationModuleException {

    public ValidationExecutionException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "VALIDATION_EXECUTION_FAILED");
    }

    public ValidationExecutionException(String message, Throwable cause) {
        super(message, cause, HttpStatus.INTERNAL_SERVER_ERROR, "VALIDATION_EXECUTION_FAILED");
    }
}
