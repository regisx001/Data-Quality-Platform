package com.regisx001.dQul.connector.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.regisx001.dQul.compute.spark.SparkSessionProvider;
import com.regisx001.dQul.connector.ConnectorConfig;
import com.regisx001.dQul.connector.api.ConnectionTestResult;
import com.regisx001.dQul.connector.api.DataType;
import com.regisx001.dQul.connector.api.DatasetType;

class PostgresDataSourceConnectorTest {

    private static final ConnectorConfig.Postgres CONFIG = new ConnectorConfig.Postgres(
            "localhost", 5432, "testdb", "public",
            "user", "pass", false, 5000, "test");

    private SparkSession spark;
    private SparkSessionProvider sparkSessionProvider;

    @BeforeEach
    void setUp() {
        spark = mock(SparkSession.class);
        sparkSessionProvider = new SparkSessionProvider(spark);
    }

    // ── testConnection (plain JDBC) ─────────────────────────────────────

    @Test
    @DisplayName("testConnection returns success when connection is valid")
    void testConnection_success() throws Exception {
        var conn = mock(Connection.class);
        when(conn.isValid(5)).thenReturn(true);

        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), any(Properties.class)))
                    .thenReturn(conn);

            var connector = new PostgresDataSourceConnector(CONFIG, sparkSessionProvider);
            ConnectionTestResult result = connector.testConnection();

            assertTrue(result.success());
            assertTrue(result.message().contains("Successfully connected"));
        }
    }

    @Test
    @DisplayName("testConnection returns failure on SQL exception")
    void testConnection_failure() {
        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), any(Properties.class)))
                    .thenAnswer(inv -> { throw new SQLException("Connection refused"); });

            var connector = new PostgresDataSourceConnector(CONFIG, sparkSessionProvider);
            ConnectionTestResult result = connector.testConnection();

            assertFalse(result.success());
            assertTrue(result.message().contains("Connection refused"));
        }
    }

    // ── discoverDatasets ────────────────────────────────────────────────

    @Test
    @DisplayName("discoverDatasets returns tables and views from information_schema")
    void discoverDatasets() throws Exception {
        var conn = mock(Connection.class);
        var stmt = mock(PreparedStatement.class);
        var rs = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);

        when(rs.getString("table_name")).thenReturn("users", "orders");
        when(rs.getString("table_type")).thenReturn("BASE TABLE", "VIEW");
        when(rs.getString("remarks")).thenReturn("Users table", "");
        when(rs.getLong("estimated_rows")).thenReturn(100L, 50L);

        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), any(Properties.class)))
                    .thenReturn(conn);

            var connector = new PostgresDataSourceConnector(CONFIG, sparkSessionProvider);
            var datasets = connector.discoverDatasets();

            assertEquals(2, datasets.size());
            assertEquals("public.users", datasets.get(0).id());
            assertEquals("users", datasets.get(0).name());
            assertEquals(DatasetType.TABLE, datasets.get(0).type());
            assertEquals("Users table", datasets.get(0).description());

            assertEquals("public.orders", datasets.get(1).id());
            assertEquals(DatasetType.VIEW, datasets.get(1).type());
        }
    }

    @Test
    @DisplayName("discoverDatasets returns empty list on error")
    void discoverDatasets_error() {
        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), any(Properties.class)))
                    .thenAnswer(inv -> { throw new SQLException("Cannot connect"); });

            var connector = new PostgresDataSourceConnector(CONFIG, sparkSessionProvider);
            var datasets = connector.discoverDatasets();

            assertTrue(datasets.isEmpty());
        }
    }

    // ── getMetadata ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getMetadata returns column info and estimated row count")
    void getMetadata() throws Exception {
        var conn = mock(Connection.class);
        var stmt = mock(PreparedStatement.class);
        var rs = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);

        when(rs.getString("column_name")).thenReturn("id", "name");
        when(rs.getString("data_type")).thenReturn("integer", "character varying");
        when(rs.getString("is_nullable")).thenReturn("NO", "YES");

        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), any(Properties.class)))
                    .thenReturn(conn);

            var connector = new PostgresDataSourceConnector(CONFIG, sparkSessionProvider);
            var metaData = connector.getMetadata("public.users");

            assertEquals("users", metaData.name());
            assertEquals(2, metaData.columns().size());

            var idCol = metaData.columns().get(0);
            assertEquals("id", idCol.name());
            assertEquals(DataType.INTEGER, idCol.type());
            assertFalse(idCol.nullable());

            var nameCol = metaData.columns().get(1);
            assertEquals("name", nameCol.name());
            assertEquals(DataType.STRING, nameCol.type());
            assertTrue(nameCol.nullable());
        }
    }

    @Test
    @DisplayName("getMetadata returns empty columns and -1 rows on error")
    void getMetadata_error() {
        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), any(Properties.class)))
                    .thenAnswer(inv -> { throw new SQLException("Cannot connect"); });

            var connector = new PostgresDataSourceConnector(CONFIG, sparkSessionProvider);
            var metaData = connector.getMetadata("public.users");

            assertEquals("users", metaData.name());
            assertTrue(metaData.columns().isEmpty());
            assertEquals(-1L, metaData.estimatedRows());
        }
    }

    // ── jdbcUrl ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("jdbcUrl is built correctly without SSL")
    void jdbcUrl_noSsl() {
        var config = new ConnectorConfig.Postgres(
                "myhost", 5432, "mydb", "public",
                "u", "p", false, 5000, "x");

        assertEquals("jdbc:postgresql://myhost:5432/mydb", config.jdbcUrl());
    }

    @Test
    @DisplayName("jdbcUrl includes SSL parameter when ssl is true")
    void jdbcUrl_withSsl() {
        var config = new ConnectorConfig.Postgres(
                "myhost", 5432, "mydb", "public",
                "u", "p", true, 5000, "x");

        assertTrue(config.jdbcUrl().contains("ssl=true"));
    }
}
