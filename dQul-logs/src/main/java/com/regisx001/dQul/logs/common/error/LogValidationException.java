package com.regisx001.dQul.logs.common.error;

/**
 * Thrown when a log event cannot be persisted because it is invalid.
 * In the Kafka consumer path this propagates to the container error handler,
 * which routes the record to the dead-letter topic.
 */
public class LogValidationException extends RuntimeException {

    public LogValidationException(String message) {
        super(message);
    }
}
