package com.regisx001.dQul.connector.exception;

import org.springframework.http.HttpStatus;

public class ConnectorConnectionException extends ConnectorModuleException {

    public ConnectorConnectionException(String message) {
        super(message, HttpStatus.BAD_GATEWAY, "CONNECTOR_CONNECTION_ERROR");
    }

    public ConnectorConnectionException(String message, Throwable cause) {
        super(message, cause, HttpStatus.BAD_GATEWAY, "CONNECTOR_CONNECTION_ERROR");
    }
}
