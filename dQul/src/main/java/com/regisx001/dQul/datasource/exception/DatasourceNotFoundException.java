package com.regisx001.dQul.datasource.exception;

import org.springframework.http.HttpStatus;

public class DatasourceNotFoundException extends DatasourceModuleException {

    public DatasourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "DATASOURCE_NOT_FOUND");
    }

    public DatasourceNotFoundException(String field, Object value) {
        super(String.format("Datasource not found with %s: %s", field, value), HttpStatus.NOT_FOUND, "DATASOURCE_NOT_FOUND");
    }
}
