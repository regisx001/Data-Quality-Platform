package com.regisx001.dQul.connector;

/**
 * Configuration for a data source connector.
 *
 * <p>This sealed type has two permitted subtypes:
 * {@link Csv} for CSV file sources and {@link Postgres} for PostgreSQL sources.
 */
public sealed interface ConnectorConfig {

    /** Human-readable name for the datasource. */
    String datasourceName();

    // ──────────────────────────────────────────────
    //  CSV configuration
    // ──────────────────────────────────────────────

    /**
     * Configuration for a CSV file datasource.
     */
    record Csv(
            String filePath,
            char delimiter,
            boolean header,
            String encoding,
            char quoteChar,
            char escapeChar,
            boolean inferSchema,
            String datasourceName) implements ConnectorConfig {

        public Csv(String filePath, String datasourceName) {
            this(filePath, ',', true, "UTF-8", '"', '\\', true, datasourceName);
        }
    }

    // ──────────────────────────────────────────────
    //  PostgreSQL configuration
    // ──────────────────────────────────────────────

    /**
     * Configuration for a PostgreSQL datasource.
     */
    record Postgres(
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

        public Postgres(
                String host, int port, String database, String schema,
                String username, String password, boolean ssl,
                int connectionTimeoutMs, String datasourceName) {
            this(host, port, database, schema, username, password, ssl,
                    connectionTimeoutMs, 10_000, datasourceName);
        }

        public Postgres(String host, String database, String username,
                        String password, String datasourceName) {
            this(host, 5432, database, "public", username, password,
                    false, 30_000, 10_000, datasourceName);
        }

        /** Builds a JDBC connection URL from the configuration. */
        public String jdbcUrl() {
            String url = String.format(
                    "jdbc:postgresql://%s:%d/%s", host, port, database);
            if (ssl) {
                url += "?ssl=true&sslmode=require";
            }
            return url;
        }
    }
}
