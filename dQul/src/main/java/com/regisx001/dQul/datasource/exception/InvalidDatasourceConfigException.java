package com.regisx001.dQul.datasource.exception;

import org.springframework.http.HttpStatus;

public class InvalidDatasourceConfigException extends DatasourceModuleException {

    public InvalidDatasourceConfigException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_DATASOURCE_CONFIG");
    }

    public InvalidDatasourceConfigException(String message, Object details) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_DATASOURCE_CONFIG", details);
    }
}
