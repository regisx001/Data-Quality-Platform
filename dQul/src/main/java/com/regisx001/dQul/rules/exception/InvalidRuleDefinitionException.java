package com.regisx001.dQul.rules.exception;

import org.springframework.http.HttpStatus;

public class InvalidRuleDefinitionException extends RuleModuleException {

    public InvalidRuleDefinitionException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_RULE_DEFINITION");
    }

    public InvalidRuleDefinitionException(String message, Object details) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_RULE_DEFINITION", details);
    }
}
