package com.regisx001.dQul.connector.api;

/**
 * Configuration for a CSV file datasource connector.
 */
public record CsvConnectorConfig(
        String filePath,
        char delimiter,
        boolean header,
        String encoding,
        char quoteChar,
        char escapeChar,
        boolean inferSchema,
        String datasourceName) implements ConnectorConfig {

    public CsvConnectorConfig(String filePath, String datasourceName) {
        this(filePath, ',', true, "UTF-8", '"', '\\', true, datasourceName);
    }
}
