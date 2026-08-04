package com.regisx001.dQul.common.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends UserModuleException {

    public UserNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
    }

    public UserNotFoundException(String field, Object value) {
        super(String.format("User not found with %s: %s", field, value), HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
    }
}
