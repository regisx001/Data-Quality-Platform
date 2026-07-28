package com.regisx001.dQul.connector.api;

/**
 * Immutable result of testing a connection to a datasource.
 *
 * @param success   {@code true} if the connection was established successfully
 * @param message   A human-readable description of the outcome
 * @param latencyMs The time, in milliseconds, that the connection attempt took
 */
public record ConnectionTestResult(boolean success, String message, long latencyMs) {

    private static final ConnectionTestResult NOT_APPLICABLE = new ConnectionTestResult(true,
            "Connection test not applicable for this datasource type", 0);

    /**
     * Returns a singleton result for datasource types that do not have
     * an actual network connection to test (e.g. local file readers).
     */
    public static ConnectionTestResult notApplicable() {
        return NOT_APPLICABLE;
    }

    /**
     * Convenience factory for a successful connection test result.
     */
    public static ConnectionTestResult success(String message, long latencyMs) {
        return new ConnectionTestResult(true, message, latencyMs);
    }

    /**
     * Convenience factory for a failed connection test result.
     */
    public static ConnectionTestResult failure(String message, long latencyMs) {
        return new ConnectionTestResult(false, message, latencyMs);
    }
}
