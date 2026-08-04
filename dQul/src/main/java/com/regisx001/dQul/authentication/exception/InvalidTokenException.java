package com.regisx001.dQul.authentication.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends AuthenticationModuleException {

    public InvalidTokenException() {
        super("Token is invalid or expired", HttpStatus.UNAUTHORIZED, "AUTH_INVALID_TOKEN");
    }

    public InvalidTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "AUTH_INVALID_TOKEN");
    }
}
