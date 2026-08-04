package com.regisx001.dQul.authentication.exception;

import org.springframework.http.HttpStatus;

public class UserDeactivatedException extends AuthenticationModuleException {

    public UserDeactivatedException() {
        super("User account is deactivated", HttpStatus.FORBIDDEN, "AUTH_USER_DEACTIVATED");
    }

    public UserDeactivatedException(String message) {
        super(message, HttpStatus.FORBIDDEN, "AUTH_USER_DEACTIVATED");
    }
}
