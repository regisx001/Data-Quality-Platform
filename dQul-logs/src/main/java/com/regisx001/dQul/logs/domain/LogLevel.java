package com.regisx001.dQul.logs.domain;

import java.util.Optional;

/**
 * Allowed log levels. Used to whitelist/validate incoming log events at consume time.
 */
public enum LogLevel {
    TRACE, DEBUG, INFO, WARN, ERROR, FATAL;

    public static Optional<LogLevel> fromString(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LogLevel.valueOf(value.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
