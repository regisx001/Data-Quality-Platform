package com.regisx001.dQul.connector.postgres;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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

    private static SparkSessionProvider sparkSessionProvider;

    @BeforeAll
    static void setUp() {
        var sparkSession = mock(SparkSession.class);
        sparkSessionProvider = new SparkSessionProvider(sparkSession);
    }

    @AfterAll
    static void tearDown() {
        // no-op
    }

    @Test
    @DisplayName("testConnection returns success when connection is valid")
    void testConnection_success() throws Exception {
        var conn = mock(Connection.class);
        when(conn.isValid(5)).thenReturn(true);

        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), any(java.util.Properties.class)))
                    .thenReturn(conn);

            var connector = new PostgresDataSourceConnector(CONFIG, sparkSessionProvider);
            ConnectionTestResult result = connector.testConnection();

            assertTrue(result.success());
            assertTrue(result.message().contains("Successfully connected"));
        }
    }

    @Test
    @DisplayName("testConnection returns failure when connection is invalid")
    void testConnection_invalid() throws Exception {
        var conn = mock(Connection.class);
        when(conn.isValid(5)).thenReturn(false);

        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), any(java.util.Properties.class)))
                    .thenReturn(conn);

            var connector = new PostgresDataSourceConnector(CONFIG, sparkSessionProvider);
            ConnectionTestResult result = connector.testConnection();

            assertFalse(result.success());
            assertTrue(result.message().contains("validation returned false"));
        }
    }

    @Test
    @DisplayName("testConnection returns failure on SQL exception")
    void testConnection_exception() throws Exception {
        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            var sqlEx = new java.sql.SQLException("Connection refused", "", 0);
            dm.when(() -> DriverManager.getConnection(anyString(), any(java.util.Properties.class)))
                    .thenThrow(sqlEx);

            var connector = new PostgresDataSourceConnector(CONFIG, sparkSessionProvider);
            ConnectionTestResult result = connector.testConnection();

            assertFalse(result.success());
            assertTrue(result.message().contains("Connection refused"));
        }
    }

    @Test
    @DisplayName("discoverDatasets returns tables and views from metadata")
    void discoverDatasets() throws Exception {
        var conn = mock(Connection.class);
        var meta = mock(DatabaseMetaData.class);
        var rs = mock(ResultSet.class);

        when(conn.getMetaData()).thenReturn(meta);
        when(meta.getTables(eq("testdb"), eq("public"), eq("%"), any(String[].class)))
                .thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getString("TABLE_NAME")).thenReturn("users", "orders");
        when(rs.getString("TABLE_TYPE")).thenReturn("TABLE", "VIEW");
        when(rs.getString("REMARKS")).thenReturn("Users table", null);

        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), any(java.util.Properties.class)))
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
    @DisplayName("getMetadata returns column info and estimated row count")
    void getMetadata() throws Exception {
        var conn = mock(Connection.class);
        var meta = mock(DatabaseMetaData.class);
        var columnsRs = mock(ResultSet.class);
        var countRs = mock(ResultSet.class);
        var countStmt = mock(Statement.class);

        when(conn.getMetaData()).thenReturn(meta);
        when(meta.getColumns("testdb", "public", "users", "%"))
                .thenReturn(columnsRs);
        when(columnsRs.next()).thenReturn(true, true, false);
        // First column: id INTEGER NOT NULL
        when(columnsRs.getString("COLUMN_NAME")).thenReturn("id", "name");
        when(columnsRs.getInt("DATA_TYPE")).thenReturn(java.sql.Types.INTEGER, java.sql.Types.VARCHAR);
        when(columnsRs.getInt("NULLABLE")).thenReturn(DatabaseMetaData.columnNoNulls, DatabaseMetaData.columnNullable);
        when(columnsRs.getInt("COLUMN_SIZE")).thenReturn(10, 255);
        when(columnsRs.wasNull()).thenReturn(false, false);
        when(columnsRs.getInt("DECIMAL_DIGITS")).thenReturn(0, 0);

        when(conn.createStatement()).thenReturn(countStmt);
        when(countStmt.executeQuery(anyString())).thenReturn(countRs);
        when(countRs.next()).thenReturn(true);
        when(countRs.getLong("estimate")).thenReturn(5000L);

        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), any(java.util.Properties.class)))
                    .thenReturn(conn);

            var connector = new PostgresDataSourceConnector(CONFIG, sparkSessionProvider);
            var metaData = connector.getMetadata("public.users");

            assertEquals("users", metaData.name());
            assertEquals(2, metaData.columns().size());
            assertEquals(5000L, metaData.estimatedRows());

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
    @DisplayName("getMetadata returns empty columns and -1 rows on SQL error")
    void getMetadata_error() throws Exception {
        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            var sqlEx = new java.sql.SQLException("Cannot connect", "", 0);
            dm.when(() -> DriverManager.getConnection(anyString(), any(java.util.Properties.class)))
                    .thenThrow(sqlEx);

            var connector = new PostgresDataSourceConnector(CONFIG, sparkSessionProvider);
            var metaData = connector.getMetadata("public.users");

            assertEquals("users", metaData.name());
            assertTrue(metaData.columns().isEmpty());
            assertEquals(-1L, metaData.estimatedRows());
        }
    }

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
