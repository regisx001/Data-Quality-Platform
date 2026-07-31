package com.regisx001.dQul.connector.postgres;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
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
    private org.apache.spark.sql.DataFrameReader reader;
    private Dataset<Row> dataset;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        spark = mock(SparkSession.class);
        sparkSessionProvider = new SparkSessionProvider(spark);

        // Mock the DataFrameReader chain
        reader = mock(org.apache.spark.sql.DataFrameReader.class);
        when(spark.read()).thenReturn(reader);
        when(reader.format(anyString())).thenReturn(reader);
        when(reader.options(anyMap())).thenReturn(reader);
        when(reader.option(anyString(), any())).thenReturn(reader);

        dataset = mock(Dataset.class);
        when(reader.load()).thenReturn(dataset);
    }

    // ── testConnection (plain JDBC) ─────────────────────────────────────

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
    @DisplayName("testConnection returns failure on SQL exception")
    void testConnection_failure() {
        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), any(java.util.Properties.class)))
                    .thenThrow(new java.sql.SQLException("Connection refused", "", 0));

            var connector = new PostgresDataSourceConnector(CONFIG, sparkSessionProvider);
            ConnectionTestResult result = connector.testConnection();

            assertFalse(result.success());
            assertTrue(result.message().contains("Connection refused"));
        }
    }

    // ── discoverDatasets ────────────────────────────────────────────────

    @Test
    @DisplayName("discoverDatasets returns tables and views from information_schema")
    void discoverDatasets() {
        var row1 = mock(Row.class);
        when(row1.getString(0)).thenReturn("users");
        when(row1.getString(1)).thenReturn("BASE TABLE");
        when(row1.getString(2)).thenReturn("Users table");

        var row2 = mock(Row.class);
        when(row2.getString(0)).thenReturn("orders");
        when(row2.getString(1)).thenReturn("VIEW");
        when(row2.getString(2)).thenReturn("");

        when(dataset.collectAsList()).thenReturn(Arrays.asList(row1, row2));

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

    @Test
    @DisplayName("discoverDatasets returns empty list on error")
    void discoverDatasets_error() {
        when(dataset.collectAsList()).thenThrow(new RuntimeException("Cannot connect"));

        var connector = new PostgresDataSourceConnector(CONFIG, sparkSessionProvider);
        var datasets = connector.discoverDatasets();

        assertTrue(datasets.isEmpty());
    }

    // ── getMetadata ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getMetadata returns column info and estimated row count")
    void getMetadata() {
        // Mock columns result
        var colRow1 = mock(Row.class);
        when(colRow1.getString(0)).thenReturn("id");
        when(colRow1.getString(1)).thenReturn("integer");
        when(colRow1.getString(2)).thenReturn("NO");

        var colRow2 = mock(Row.class);
        when(colRow2.getString(0)).thenReturn("name");
        when(colRow2.getString(1)).thenReturn("character varying");
        when(colRow2.getString(2)).thenReturn("YES");

        // Mock row count result
        var countRow = mock(Row.class);
        when(countRow.isNullAt(0)).thenReturn(false);
        when(countRow.getLong(0)).thenReturn(5000L);

        // Return columns dataset first, then count dataset
        Dataset<Row> colDataset = mock(Dataset.class);
        when(colDataset.collectAsList()).thenReturn(Arrays.asList(colRow1, colRow2));

        Dataset<Row> countDataset = mock(Dataset.class);
        when(countDataset.head()).thenReturn(countRow);

        // First load() call returns colDataset, second returns countDataset
        when(reader.load())
                .thenReturn(colDataset)
                .thenReturn(countDataset);

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

    @Test
    @DisplayName("getMetadata returns empty columns and -1 rows on error")
    void getMetadata_error() {
        when(reader.load()).thenThrow(new RuntimeException("Cannot connect"));

        var connector = new PostgresDataSourceConnector(CONFIG, sparkSessionProvider);
        var metaData = connector.getMetadata("public.users");

        assertEquals("users", metaData.name());
        assertTrue(metaData.columns().isEmpty());
        assertEquals(-1L, metaData.estimatedRows());
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
