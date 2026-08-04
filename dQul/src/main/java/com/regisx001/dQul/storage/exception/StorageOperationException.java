package com.regisx001.dQul.storage.exception;

import org.springframework.http.HttpStatus;

public class StorageOperationException extends StorageModuleException {

    public StorageOperationException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_OPERATION_FAILED");
    }

    public StorageOperationException(String message, Throwable cause) {
        super(message, cause, HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_OPERATION_FAILED");
    }
}
