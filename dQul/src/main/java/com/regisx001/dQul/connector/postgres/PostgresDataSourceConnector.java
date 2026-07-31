package com.regisx001.dQul.connector.postgres;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.regisx001.dQul.compute.spark.SparkSessionProvider;
import com.regisx001.dQul.connector.ConnectorConfig;
import com.regisx001.dQul.connector.DataSourceConnector;
import com.regisx001.dQul.connector.api.ColumnMetadata;
import com.regisx001.dQul.connector.api.ConnectionTestResult;
import com.regisx001.dQul.connector.api.DataType;
import com.regisx001.dQul.connector.api.DatasetDescriptor;
import com.regisx001.dQul.connector.api.DatasetMetadata;
import com.regisx001.dQul.connector.api.DatasetType;

public class PostgresDataSourceConnector implements DataSourceConnector {

    private static final Logger log = LoggerFactory.getLogger(PostgresDataSourceConnector.class);

    private final ConnectorConfig.Postgres config;
    private final SparkSessionProvider sparkSessionProvider;

    public PostgresDataSourceConnector(ConnectorConfig.Postgres config, SparkSessionProvider sparkSessionProvider) {
        this.config = config;
        this.sparkSessionProvider = sparkSessionProvider;
    }

    // ── Plain JDBC for lightweight operations ────────────────────────────

    private Connection openConnection() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", config.username());
        props.setProperty("password", config.password());
        if (config.ssl()) {
            props.setProperty("ssl", "true");
            props.setProperty("sslmode", "require");
        }
        // Build URL with connection timeout
        String url = config.jdbcUrl();
        String timeoutParam = "connectTimeout=%d".formatted(config.connectionTimeoutMs() / 1000);
        url += url.contains("?") ? "&" + timeoutParam : "?" + timeoutParam;
        return DriverManager.getConnection(url, props);
    }

    // ── Spark JDBC reader helpers ────────────────────────────────────────

    private java.util.Map<String, String> jdbcOptions() {
        java.util.Map<String, String> opts = new java.util.HashMap<>();
        opts.put("url", config.jdbcUrl());
        opts.put("user", config.username());
        opts.put("password", config.password());
        opts.put("fetchSize", String.valueOf(config.fetchSize()));
        opts.put("pushDownPredicate", "true");
        if (config.ssl()) {
            opts.put("ssl", "true");
            opts.put("sslmode", "require");
        }
        return opts;
    }

    private Dataset<Row> sparkQuery(String sql) {
        SparkSession spark = sparkSessionProvider.get();
        return spark.read()
                .format("jdbc")
                .options(jdbcOptions())
                .option("query", sql)
                .load();
    }

    // ── Connection testing (plain JDBC — lightweight) ────────────────────

    @Override
    public ConnectionTestResult testConnection() {
        long start = System.currentTimeMillis();
        log.info("Testing PostgreSQL connection to {}:{}", config.host(), config.port());
        try (Connection conn = openConnection()) {
            boolean valid = conn.isValid(5);
            long elapsed = System.currentTimeMillis() - start;
            if (valid) {
                return ConnectionTestResult.success(
                        "Successfully connected to PostgreSQL at %s:%d/%s"
                                .formatted(config.host(), config.port(), config.database()),
                        elapsed);
            }
            return ConnectionTestResult.failure(
                    "PostgreSQL connection validation returned false", elapsed);
        } catch (SQLException e) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("PostgreSQL connection test failed: {}", e.getMessage());
            return ConnectionTestResult.failure(
                    "PostgreSQL connection failed: " + e.getMessage(), elapsed);
        }
    }

    // ── Dataset discovery (Spark) ────────────────────────────────────────

    @Override
    public List<DatasetDescriptor> discoverDatasets() {
        List<DatasetDescriptor> descriptors = new ArrayList<>();

        try {
            String sql = "SELECT table_name, table_type, "
                    + "COALESCE(obj_description('%s.'.catalog || table_name::regclass, 'pg_class'), '') AS remarks "
                            .formatted(escapeLiteral(config.schema()))
                    + "FROM information_schema.tables "
                    + "WHERE table_schema = '%s' "
                            .formatted(escapeLiteral(config.schema()))
                    + "AND table_type IN ('BASE TABLE', 'VIEW') "
                    + "ORDER BY table_name";

            log.info("Discovering datasets via Spark JDBC query");
            Dataset<Row> df = sparkQuery(sql);

            for (Row row : df.collectAsList()) {
                String tableName = row.getString(0);
                String tableType = row.getString(1);
                String remarks = row.getString(2);

                DatasetType datasetType = "VIEW".equalsIgnoreCase(tableType)
                        ? DatasetType.VIEW
                        : DatasetType.TABLE;

                String id = "%s.%s".formatted(config.schema(), tableName);

                descriptors.add(new DatasetDescriptor(
                        id, tableName, datasetType, remarks));
            }

            log.info("Discovered {} datasets in schema '{}' of database '{}'",
                    descriptors.size(), config.schema(), config.database());

        } catch (Exception e) {
            log.error("Failed to discover datasets in PostgreSQL: {}", e.getMessage(), e);
        }

        return descriptors;
    }

    // ── Metadata retrieval (Spark) ───────────────────────────────────────

    @Override
    public DatasetMetadata getMetadata(String datasetId) {
        String[] parts = datasetId.split("\\.", 2);
        String schema = parts.length > 1 ? parts[0] : config.schema();
        String tableName = parts.length > 1 ? parts[1] : datasetId;

        List<ColumnMetadata> columns = new ArrayList<>();
        long estimatedRows = -1;

        try {
            // Column metadata via Spark
            String colSql = "SELECT column_name, data_type, "
                    + "is_nullable "
                    + "FROM information_schema.columns "
                    + "WHERE table_schema = '%s' AND table_name = '%s' "
                            .formatted(escapeLiteral(schema), escapeLiteral(tableName))
                    + "ORDER BY ordinal_position";

            log.info("Retrieving column metadata via Spark JDBC query");
            Dataset<Row> colDf = sparkQuery(colSql);

            for (Row row : colDf.collectAsList()) {
                String colName = row.getString(0);
                String pgDataType = row.getString(1);
                String nullableStr = row.getString(2);

                columns.add(new ColumnMetadata(
                        colName,
                        mapPgDataType(pgDataType),
                        "YES".equalsIgnoreCase(nullableStr)));
            }

            // Row count estimate via Spark
            String countSql = "SELECT reltuples::bigint AS estimate "
                    + "FROM pg_class WHERE relnamespace = "
                    + "(SELECT oid FROM pg_namespace WHERE nspname = '%s') "
                            .formatted(escapeLiteral(schema))
                    + "AND relname = '%s'".formatted(escapeLiteral(tableName));

            log.info("Retrieving row count estimate via Spark JDBC query");
            Dataset<Row> countDf = sparkQuery(countSql);
            Row countRow = countDf.head();
            if (countRow != null && !countRow.isNullAt(0)) {
                estimatedRows = countRow.getLong(0);
            }

        } catch (Exception e) {
            log.error("Failed to retrieve metadata for '{}': {}",
                    datasetId, e.getMessage(), e);
        }

        return new DatasetMetadata(tableName, columns, estimatedRows);
    }

    // ── Data reader (Spark) ──────────────────────────────────────────────

    @Override
    public DataReader createReader(String datasetId) {
        String[] parts = datasetId.split("\\.", 2);
        String schema = parts.length > 1 ? parts[0] : config.schema();
        String tableName = parts.length > 1 ? parts[1] : datasetId;

        return () -> {
            SparkSession spark = sparkSessionProvider.get();
            return spark.read()
                    .format("jdbc")
                    .options(jdbcOptions())
                    .option("dbtable", "%s.%s".formatted(schema, tableName))
                    .load();
        };
    }

    // ── Type mapping ────────────────────────────────────────────────────

    private static DataType mapPgDataType(String pgType) {
        if (pgType == null)
            return DataType.UNKNOWN;
        return switch (pgType.toLowerCase()) {
            case "character varying", "character", "varchar",
                    "char", "text", "name", "\"char\"" ->
                DataType.STRING;

            case "smallint", "integer", "int", "int4", "serial",
                    "smallserial" ->
                DataType.INTEGER;

            case "bigint", "bigserial", "int8", "oid" ->
                DataType.LONG;

            case "real", "float4", "double precision", "float8", "float" ->
                DataType.DOUBLE;

            case "numeric", "decimal" ->
                DataType.DECIMAL;

            case "boolean", "bool" ->
                DataType.BOOLEAN;

            case "date" ->
                DataType.DATE;

            case "timestamp without time zone", "timestamp with time zone",
                    "timestamp", "timestamptz",
                    "time without time zone", "time with time zone",
                    "time", "timetz" ->
                DataType.TIMESTAMP;

            case "bytea", "bit", "bit varying" ->
                DataType.BINARY;

            case "array", "json", "jsonb" ->
                DataType.STRING;

            default -> {
                log.debug("Unmapped PostgreSQL type '{}', falling back to UNKNOWN", pgType);
                yield DataType.UNKNOWN;
            }
        };
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private static String escapeLiteral(String s) {
        return s.replace("'", "''");
    }
}
