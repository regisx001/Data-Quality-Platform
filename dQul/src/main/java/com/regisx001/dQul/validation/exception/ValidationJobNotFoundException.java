package com.regisx001.dQul.validation.exception;

import org.springframework.http.HttpStatus;

public class ValidationJobNotFoundException extends ValidationModuleException {

    public ValidationJobNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "VALIDATION_JOB_NOT_FOUND");
    }

    public ValidationJobNotFoundException(String field, Object value) {
        super(String.format("Validation job execution not found with %s: %s", field, value), HttpStatus.NOT_FOUND, "VALIDATION_JOB_NOT_FOUND");
    }
}
