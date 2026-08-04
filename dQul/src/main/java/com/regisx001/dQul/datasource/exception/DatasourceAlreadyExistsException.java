package com.regisx001.dQul.datasource.exception;

import org.springframework.http.HttpStatus;

public class DatasourceAlreadyExistsException extends DatasourceModuleException {

    public DatasourceAlreadyExistsException(String name) {
        super("Datasource with name '" + name + "' already exists", HttpStatus.CONFLICT, "DATASOURCE_ALREADY_EXISTS");
    }
}
