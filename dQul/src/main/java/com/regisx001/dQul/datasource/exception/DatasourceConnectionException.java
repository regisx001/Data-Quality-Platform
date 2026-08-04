package com.regisx001.dQul.datasource.exception;

import org.springframework.http.HttpStatus;

public class DatasourceConnectionException extends DatasourceModuleException {

    public DatasourceConnectionException(String message) {
        super(message, HttpStatus.BAD_GATEWAY, "DATASOURCE_CONNECTION_FAILED");
    }

    public DatasourceConnectionException(String message, Throwable cause) {
        super(message, cause, HttpStatus.BAD_GATEWAY, "DATASOURCE_CONNECTION_FAILED");
    }
}
