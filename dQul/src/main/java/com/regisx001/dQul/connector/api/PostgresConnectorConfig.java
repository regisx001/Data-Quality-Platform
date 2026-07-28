package com.regisx001.dQul.connector.api;

/**
 * Configuration for a PostgreSQL datasource connector.
 *
 * @param host                The PostgreSQL server hostname or IP address
 * @param port                The PostgreSQL server port (default 5432)
 * @param database            The target database name
 * @param schema              The database schema to scope discovery to (e.g.
 *                            {@code public})
 * @param username            The authentication username
 * @param password            The authentication password
 * @param ssl                 Whether to connect using SSL/TLS
 * @param connectionTimeoutMs Connection timeout in milliseconds
 * @param datasourceName      A human-readable name for this datasource
 *                            configuration
 */
public record PostgresConnectorConfig(
        String host,
        int port,
        String database,
        String schema,
        String username,
        String password,
        boolean ssl,
        int connectionTimeoutMs,
        String datasourceName) implements ConnectorConfig {

    /**
     * Creates a PostgresConnectorConfig with default port (5432),
     * default schema (public), SSL disabled, and a 30-second timeout.
     */
    public PostgresConnectorConfig(
            String host,
            String database,
            String username,
            String password,
            String datasourceName) {
        this(host, 5432, database, "public", username, password,
                false, 30_000, datasourceName);
    }

    /**
     * Returns the JDBC connection URL constructed from this configuration.
     */
    public String jdbcUrl() {
        String url = String.format(
                "jdbc:postgresql://%s:%d/%s",
                host, port, database);
        if (ssl) {
            url += "?ssl=true&sslmode=require";
        }
        return url;
    }
}
