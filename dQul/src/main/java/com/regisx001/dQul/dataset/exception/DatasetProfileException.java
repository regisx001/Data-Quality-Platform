package com.regisx001.dQul.dataset.exception;

import org.springframework.http.HttpStatus;

public class DatasetProfileException extends DatasetModuleException {

    public DatasetProfileException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "DATASET_PROFILE_FAILED");
    }

    public DatasetProfileException(String message, Throwable cause) {
        super(message, cause, HttpStatus.INTERNAL_SERVER_ERROR, "DATASET_PROFILE_FAILED");
    }
}
