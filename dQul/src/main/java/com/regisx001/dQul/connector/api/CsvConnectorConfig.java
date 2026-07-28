package com.regisx001.dQul.connector.api;

/**
 * Configuration for a CSV file datasource connector.
 *
 * @param filePath       The absolute or relative path to the CSV file
 * @param delimiter      The field delimiter character (default {@code ,})
 * @param header         Whether the first line contains column headers
 * @param encoding       The file encoding (default {@code UTF-8})
 * @param quoteChar      The character used for quoting fields (default
 *                       {@code "})
 * @param escapeChar     The escape character (default {@code \\})
 * @param inferSchema    Whether to infer column data types from the data
 * @param datasourceName A human-readable name for this datasource configuration
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

    /**
     * Creates a CsvConnectorConfig with sensible defaults:
     * comma delimiter, first-row header, UTF-8 encoding,
     * double-quote quoting, backslash escaping, and schema inference enabled.
     */
    public CsvConnectorConfig(String filePath, String datasourceName) {
        this(filePath, ',', true, "UTF-8", '"', '\\', true, datasourceName);
    }
}
