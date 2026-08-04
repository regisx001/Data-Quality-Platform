package com.regisx001.dQul.connector.exception;

import org.springframework.http.HttpStatus;

public class ConnectorNotFoundException extends ConnectorModuleException {

    public ConnectorNotFoundException(String type) {
        super("No connector registered for type: " + type, HttpStatus.NOT_FOUND, "CONNECTOR_NOT_FOUND");
    }
}
