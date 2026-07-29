package com.regisx001.dQul.connector.api;

/**
 * Sealed marker interface for all connector configuration types.
 */
public sealed interface ConnectorConfig
        permits PostgresConnectorConfig, CsvConnectorConfig {

    /**
     * Returns a human-readable name for the datasource that this
     * configuration belongs to. Used for logging and identification.
     */
    String datasourceName();
}
