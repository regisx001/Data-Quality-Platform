package com.regisx001.dQul.rules.exception;

import org.springframework.http.HttpStatus;

public class RuleNotFoundException extends RuleModuleException {

    public RuleNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RULE_NOT_FOUND");
    }

    public RuleNotFoundException(String field, Object value) {
        super(String.format("Rule not found with %s: %s", field, value), HttpStatus.NOT_FOUND, "RULE_NOT_FOUND");
    }
}
