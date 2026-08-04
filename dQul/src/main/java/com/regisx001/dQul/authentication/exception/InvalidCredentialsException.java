package com.regisx001.dQul.authentication.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends AuthenticationModuleException {

    public InvalidCredentialsException() {
        super("Invalid username or password", HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS");
    }

    public InvalidCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS");
    }
}
