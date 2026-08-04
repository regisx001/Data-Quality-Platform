package com.regisx001.dQul.dataset.exception;

import org.springframework.http.HttpStatus;

public class InvalidDatasetConfigException extends DatasetModuleException {

    public InvalidDatasetConfigException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_DATASET_CONFIG");
    }
}
