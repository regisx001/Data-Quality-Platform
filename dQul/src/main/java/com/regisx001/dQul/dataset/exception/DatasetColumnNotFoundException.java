package com.regisx001.dQul.dataset.exception;

import org.springframework.http.HttpStatus;

public class DatasetColumnNotFoundException extends DatasetModuleException {

    public DatasetColumnNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "DATASET_COLUMN_NOT_FOUND");
    }

    public DatasetColumnNotFoundException(String columnId, String datasetId) {
        super(String.format("Column %s not found in dataset %s", columnId, datasetId), HttpStatus.NOT_FOUND, "DATASET_COLUMN_NOT_FOUND");
    }
}
