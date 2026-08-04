package com.regisx001.dQul.compute.exception;

import org.springframework.http.HttpStatus;

public class SparkSessionException extends ComputeModuleException {

    public SparkSessionException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, "SPARK_SESSION_ERROR");
    }

    public SparkSessionException(String message, Throwable cause) {
        super(message, cause, HttpStatus.SERVICE_UNAVAILABLE, "SPARK_SESSION_ERROR");
    }
}
