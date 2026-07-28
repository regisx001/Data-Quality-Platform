package com.regisx001.dQul.connector.postgres;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.regisx001.dQul.compute.spark.SparkSessionProvider;
import com.regisx001.dQul.connector.api.ColumnMetadata;
import com.regisx001.dQul.connector.api.ConnectionTestResult;
import com.regisx001.dQul.connector.api.DataReader;
import com.regisx001.dQul.connector.api.DataSourceConnector;
import com.regisx001.dQul.connector.api.DataType;
import com.regisx001.dQul.connector.api.DatasetDescriptor;
import com.regisx001.dQul.connector.api.DatasetMetadata;
import com.regisx001.dQul.connector.api.DatasetType;
import com.regisx001.dQul.connector.api.PostgresConnectorConfig;

/**
 * PostgreSQL connector that exposes two faces:
 *
 * <p>
 * <b>Metadata API</b> — Uses JDBC {@link DatabaseMetaData} and
 * {@code information_schema} / {@code pg_catalog} queries for rich dataset
 * discovery, schema extraction, and statistics. This is the face consumed
 * by the UI and metadata services.
 *
 * <p>
 * <b>Compute API</b> — Produces a Spark {@link DataReader} backed by the
 * Spark JDBC data source, keeping data in its native location until the
 * compute engine triggers a read. This is the face consumed by the
 * Validation Engine and profiling pipelines.
 */
public class PostgresDataSourceConnector implements DataSourceConnector {

    private static final Logger log = LoggerFactory.getLogger(PostgresDataSourceConnector.class);

    private final PostgresConnectorConfig config;
    private final SparkSessionProvider sparkSessionProvider;

    public PostgresDataSourceConnector(PostgresConnectorConfig config, SparkSessionProvider sparkSessionProvider) {
        this.config = config;
        this.sparkSessionProvider = sparkSessionProvider;
    }

    // ── Connection management ───────────────────────────────────────────

    private Connection openConnection() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", config.username());
        props.setProperty("password", config.password());
        props.setProperty("loginTimeout",
                String.valueOf(config.connectionTimeoutMs() / 1000));
        if (config.ssl()) {
            props.setProperty("ssl", "true");
            props.setProperty("sslmode", "require");
        }
        return DriverManager.getConnection(config.jdbcUrl(), props);
    }

    // ── MetadataApi / ComputeApi ────────────────────────────────────────

    @Override
    public ConnectionTestResult testConnection() {
        long start = System.currentTimeMillis();
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
            return ConnectionTestResult.failure(
                    "PostgreSQL connection failed: " + e.getMessage(), elapsed);
        }
    }

    @Override
    public List<DatasetDescriptor> discoverDatasets() {
        List<DatasetDescriptor> descriptors = new ArrayList<>();
        String[] types = { "TABLE", "VIEW" };

        try (Connection conn = openConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(
                    config.database(),
                    config.schema(),
                    "%",
                    types)) {

                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String tableType = rs.getString("TABLE_TYPE");
                    String remarks = rs.getString("REMARKS");

                    DatasetType datasetType = "VIEW".equalsIgnoreCase(tableType)
                            ? DatasetType.VIEW
                            : DatasetType.TABLE;

                    // Use fully-qualified name as the stable identifier
                    String id = "%s.%s".formatted(config.schema(), tableName);

                    descriptors.add(new DatasetDescriptor(
                            id, tableName, datasetType, remarks));
                }
            }
            log.info("Discovered {} datasets in schema '{}' of database '{}'",
                    descriptors.size(), config.schema(), config.database());

        } catch (SQLException e) {
            log.error("Failed to discover datasets in PostgreSQL: {}", e.getMessage(), e);
        }

        return descriptors;
    }

    @Override
    public DatasetMetadata getMetadata(String datasetId) {
        String[] parts = datasetId.split("\\.", 2);
        String schema = parts.length > 1 ? parts[0] : config.schema();
        String tableName = parts.length > 1 ? parts[1] : datasetId;

        List<ColumnMetadata> columns = new ArrayList<>();
        long estimatedRows = -1;

        try (Connection conn = openConnection()) {
            // ── Column metadata ──────────────────────────────────────────
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(
                    config.database(), schema, tableName, "%")) {

                while (rs.next()) {
                    String colName = rs.getString("COLUMN_NAME");
                    int sqlType = rs.getInt("DATA_TYPE");
                    int nullableFlag = rs.getInt("NULLABLE");
                    int precision = rs.getInt("COLUMN_SIZE");
                    if (rs.wasNull()) {
                        precision = -1;
                    }
                    int scale = rs.getInt("DECIMAL_DIGITS");
                    if (rs.wasNull()) {
                        scale = -1;
                    }

                    columns.add(new ColumnMetadata(
                            colName,
                            mapSqlType(sqlType),
                            nullableFlag == DatabaseMetaData.columnNullable,
                            precision >= 0 ? precision : null,
                            scale >= 0 ? scale : null));
                }
            }

            // ── Estimated row count ──────────────────────────────────────
            String countSql = "SELECT reltuples::bigint AS estimate "
                    + "FROM pg_class WHERE relnamespace = "
                    + "(SELECT oid FROM pg_namespace WHERE nspname = '%s') "
                    + "AND relname = '%s'".formatted(
                            escapeIdentifier(schema), escapeIdentifier(tableName));

            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(countSql)) {
                if (rs.next()) {
                    estimatedRows = rs.getLong("estimate");
                }
            }

        } catch (SQLException e) {
            log.error("Failed to retrieve metadata for '{}': {}",
                    datasetId, e.getMessage(), e);
        }

        return new DatasetMetadata(tableName, columns, estimatedRows);
    }

    @Override
    public DataReader createReader(String datasetId) {
        String[] parts = datasetId.split("\\.", 2);
        String schema = parts.length > 1 ? parts[0] : config.schema();
        String tableName = parts.length > 1 ? parts[1] : datasetId;

        return () -> sparkSessionProvider.get().read()
                .format("jdbc")
                .option("url", config.jdbcUrl())
                .option("user", config.username())
                .option("password", config.password())
                .option("dbtable", "%s.%s".formatted(schema, tableName))
                .option("fetchSize", "10000")
                .option("pushDownPredicate", "true")
                .load();
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    /**
     * Maps a {@link java.sql.Types} constant to the canonical {@link DataType}.
     */
    private static DataType mapSqlType(int sqlType) {
        return switch (sqlType) {
            case java.sql.Types.CHAR, java.sql.Types.VARCHAR,
                    java.sql.Types.LONGVARCHAR, java.sql.Types.NCHAR,
                    java.sql.Types.NVARCHAR, java.sql.Types.LONGNVARCHAR,
                    java.sql.Types.CLOB, java.sql.Types.NCLOB ->
                DataType.STRING;

            case java.sql.Types.TINYINT, java.sql.Types.SMALLINT,
                    java.sql.Types.INTEGER ->
                DataType.INTEGER;

            case java.sql.Types.BIGINT -> DataType.LONG;

            case java.sql.Types.REAL, java.sql.Types.FLOAT,
                    java.sql.Types.DOUBLE ->
                DataType.DOUBLE;

            case java.sql.Types.DECIMAL, java.sql.Types.NUMERIC -> DataType.DECIMAL;

            case java.sql.Types.BIT, java.sql.Types.BOOLEAN -> DataType.BOOLEAN;

            case java.sql.Types.DATE -> DataType.DATE;

            case java.sql.Types.TIMESTAMP, java.sql.Types.TIMESTAMP_WITH_TIMEZONE,
                    java.sql.Types.TIME, java.sql.Types.TIME_WITH_TIMEZONE ->
                DataType.TIMESTAMP;

            case java.sql.Types.BINARY, java.sql.Types.VARBINARY,
                    java.sql.Types.LONGVARBINARY, java.sql.Types.BLOB ->
                DataType.BINARY;

            case java.sql.Types.ARRAY -> DataType.ARRAY;

            case java.sql.Types.STRUCT, java.sql.Types.JAVA_OBJECT -> DataType.STRUCT;

            default -> DataType.UNKNOWN;
        };
    }

    /**
     * Safely escapes a SQL identifier to prevent SQL injection in metadata queries.
     */
    private static String escapeIdentifier(String identifier) {
        return identifier.replace("'", "''");
    }
}
