package com.regisx001.dQul.connector;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Provides configuration schema definitions for each connector type.
 * The frontend consumes these schemas to render dynamic configuration forms.
 */
@Service
public class ConnectorConfigSchemaService {

    private static final List<ConnectorConfigSchema> SCHEMAS = List.of(
            new ConnectorConfigSchema(
                    "POSTGRESQL",
                    "PostgreSQL",
                    "Configuration for a PostgreSQL database connection.",
                    List.of(
                            field("host", "Host", "string", "localhost",
                                    "Database server hostname or IP address.", true, null, null, null),
                            field("port", "Port", "number", "5432",
                                    "Database server port.", true, 1, 65535, null),
                            field("database", "Database", "string", null,
                                    "Name of the database to connect to.", true, null, null, null),
                            field("schema", "Schema", "string", "public",
                                    "Database schema to use.", false, null, null, null),
                            field("username", "Username", "string", null,
                                    "Database authentication username.", true, null, null, null),
                            field("password", "Password", "password", null,
                                    "Database authentication password.", true, null, null, null),
                            field("ssl", "SSL", "boolean", "false",
                                    "Enable SSL/TLS for the connection.", false, null, null, null),
                            field("connectionTimeoutMs", "Connection Timeout (ms)", "number", "30000",
                                    "Maximum wait time for a connection in milliseconds.", false, 1000, null, null),
                            field("fetchSize", "Fetch Size", "number", "10000",
                                    "Number of rows to fetch per database round trip.", false, 1, null, null))),

            new ConnectorConfigSchema(
                    "CSV",
                    "CSV File",
                    "Configuration for a CSV file datasource.",
                    List.of(
                            field("filePath", "File Path", "string", null,
                                    "Absolute path to the CSV file on the server.", true, null, null, null),
                            field("delimiter", "Delimiter", "string", ",",
                                    "Character used to separate fields.", false, null, null, null),
                            field("header", "Header Row", "boolean", "true",
                                    "Whether the first row contains column headers.", false, null, null, null),
                            field("encoding", "Encoding", "string", "UTF-8",
                                    "Character encoding of the file.", false, null, null,
                                    List.of("UTF-8", "UTF-16", "ISO-8859-1", "US-ASCII", "Windows-1252")),
                            field("quoteChar", "Quote Character", "string", "\"",
                                    "Character used for quoting fields.", false, null, null, null),
                            field("escapeChar", "Escape Character", "string", "\\",
                                    "Character used for escaping special characters.", false, null, null, null),
                            field("inferSchema", "Infer Schema", "boolean", "true",
                                    "Automatically detect column data types.", false, null, null, null))));

    private static ConnectorConfigSchema.ConfigField field(
            String name, String label, String type, String defaultValue,
            String description, boolean required, Integer min, Integer max,
            List<String> options) {
        return new ConnectorConfigSchema.ConfigField(
                name, label, type, defaultValue, description, required, min, max, options);
    }

    /**
     * Returns the configuration schema for a specific connector type.
     *
     * @param type the connector type (e.g., "POSTGRESQL", "CSV")
     * @return the schema, or null if the type is unknown
     */
    public ConnectorConfigSchema getSchema(String type) {
        return SCHEMAS.stream()
                .filter(s -> s.type().equalsIgnoreCase(type))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns all available configuration schemas.
     */
    public List<ConnectorConfigSchema> getAllSchemas() {
        return SCHEMAS;
    }
}
