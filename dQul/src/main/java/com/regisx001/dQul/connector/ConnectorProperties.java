package com.regisx001.dQul.connector;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for data source connectors.
 * Bound automatically from 'connector.*' in application.yaml.
 */
@Data
@Component
@ConfigurationProperties(prefix = "connector")
public class ConnectorProperties {

    private Postgres postgres = new Postgres();
    private Csv csv = new Csv();

    /**
     * PostgreSQL connector default properties.
     */
    @Data
    public static class Postgres {

        /**
         * Hostname or IP address of the PostgreSQL server.
         */
        private String host = "localhost";

        /**
         * Port of the PostgreSQL server.
         */
        private int port = 5432;

        /**
         * Target database name.
         */
        private String database = "postgres";

        /**
         * Default database schema.
         */
        private String schema = "public";

        /**
         * Authentication username.
         */
        private String username = "postgres";

        /**
         * Authentication password.
         */
        private String password = "postgres";

        /**
         * Whether SSL connection is enabled.
         */
        private boolean ssl = false;

        /**
         * Connection timeout in milliseconds.
         */
        private int connectionTimeoutMs = 30000;

        /**
         * JDBC fetch size for reading records.
         */
        private int fetchSize = 10000;
    }

    /**
     * CSV connector default properties.
     */
    @Data
    public static class Csv {

        /**
         * Field delimiter character.
         */
        private char delimiter = ',';

        /**
         * Whether the CSV file includes a header line.
         */
        private boolean header = true;

        /**
         * Character encoding of the CSV file.
         */
        private String encoding = "UTF-8";

        /**
         * Character used for field quoting.
         */
        private char quoteChar = '"';

        /**
         * Character used for escaping special characters.
         */
        private char escapeChar = '\\';

        /**
         * Whether Spark should automatically infer column data types.
         */
        private boolean inferSchema = true;
    }
}
