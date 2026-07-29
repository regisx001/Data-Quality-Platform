package com.regisx001.dQul.connector.api;

/**
 * Configuration for a PostgreSQL datasource connector.
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
        int fetchSize,
        String datasourceName) implements ConnectorConfig {

    public PostgresConnectorConfig(
            String host,
            int port,
            String database,
            String schema,
            String username,
            String password,
            boolean ssl,
            int connectionTimeoutMs,
            String datasourceName) {
        this(host, port, database, schema, username, password, ssl, connectionTimeoutMs, 10_000, datasourceName);
    }

    public PostgresConnectorConfig(
            String host,
            String database,
            String username,
            String password,
            String datasourceName) {
        this(host, 5432, database, "public", username, password,
                false, 30_000, 10_000, datasourceName);
    }

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
