package com.regisx001.dQul.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidUserDataException extends UserModuleException {

    public InvalidUserDataException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_USER_DATA");
    }

    public InvalidUserDataException(String message, Object details) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_USER_DATA", details);
    }
}
