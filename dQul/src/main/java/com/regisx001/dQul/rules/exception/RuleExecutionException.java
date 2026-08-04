package com.regisx001.dQul.rules.exception;

import org.springframework.http.HttpStatus;

public class RuleExecutionException extends RuleModuleException {

    public RuleExecutionException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "RULE_EXECUTION_FAILED");
    }

    public RuleExecutionException(String message, Throwable cause) {
        super(message, cause, HttpStatus.INTERNAL_SERVER_ERROR, "RULE_EXECUTION_FAILED");
    }
}
