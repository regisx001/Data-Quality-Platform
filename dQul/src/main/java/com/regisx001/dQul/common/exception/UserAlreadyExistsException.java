package com.regisx001.dQul.common.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends UserModuleException {

    public UserAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT, "USER_ALREADY_EXISTS");
    }
}
