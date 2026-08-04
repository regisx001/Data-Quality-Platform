package com.regisx001.dQul.validation.exception;

import org.springframework.http.HttpStatus;

public class ValidationSuiteNotFoundException extends ValidationModuleException {

    public ValidationSuiteNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "VALIDATION_SUITE_NOT_FOUND");
    }

    public ValidationSuiteNotFoundException(String field, Object value) {
        super(String.format("Validation suite not found with %s: %s", field, value), HttpStatus.NOT_FOUND, "VALIDATION_SUITE_NOT_FOUND");
    }
}
