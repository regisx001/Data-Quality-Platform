package com.regisx001.dQul.dataset.exception;

import org.springframework.http.HttpStatus;

public class DatasetNotFoundException extends DatasetModuleException {

    public DatasetNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "DATASET_NOT_FOUND");
    }

    public DatasetNotFoundException(String field, Object value) {
        super(String.format("Dataset not found with %s: %s", field, value), HttpStatus.NOT_FOUND, "DATASET_NOT_FOUND");
    }
}
