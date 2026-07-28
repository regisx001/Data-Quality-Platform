package com.regisx001.dQul.connector.api;

/**
 * Sealed marker interface for all connector configuration types.
 * The {@link ConnectorFactory} uses pattern-matching on this type to
 * instantiate the appropriate connector implementation.
 *
 * <p>
 * Every concrete datasource connector defines its own configuration
 * record that implements this interface.
 */
public sealed interface ConnectorConfig
        permits PostgresConnectorConfig, CsvConnectorConfig {

    /**
     * Returns a human-readable name for the datasource that this
     * configuration belongs to. Used for logging and identification.
     */
    String datasourceName();
}
