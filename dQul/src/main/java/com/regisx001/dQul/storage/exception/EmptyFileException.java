package com.regisx001.dQul.storage.exception;

import org.springframework.http.HttpStatus;

public class EmptyFileException extends StorageModuleException {

    public EmptyFileException() {
        super("Cannot upload empty or null file", HttpStatus.BAD_REQUEST, "STORAGE_FILE_EMPTY");
    }

    public EmptyFileException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "STORAGE_FILE_EMPTY");
    }
}
