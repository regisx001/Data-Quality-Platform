package com.regisx001.dQul.connector;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regisx001.dQul.compute.spark.SparkSessionProvider;
import com.regisx001.dQul.connector.csv.CsvDataSourceConnector;
import com.regisx001.dQul.connector.postgres.PostgresDataSourceConnector;

class ConnectorFactoryTest {

    private final SparkSessionProvider sparkSessionProvider = mock(SparkSessionProvider.class);

    @Test
    @DisplayName("createConnector with Postgres config returns PostgresDataSourceConnector")
    void createConnector_postgres() {
        var factory = new ConnectorFactory(sparkSessionProvider);
        var config = new ConnectorConfig.Postgres("h", "d", "u", "p", "pg");

        var connector = factory.createConnector(config);

        assertInstanceOf(PostgresDataSourceConnector.class, connector);
    }

    @Test
    @DisplayName("createConnector with Csv config returns CsvDataSourceConnector")
    void createConnector_csv() {
        var factory = new ConnectorFactory(sparkSessionProvider);
        var config = new ConnectorConfig.Csv("/path/to/file.csv", "csv");

        var connector = factory.createConnector(config);

        assertInstanceOf(CsvDataSourceConnector.class, connector);
    }

    @Test
    @DisplayName("createDefaultPostgresConfig populates from ConnectorProperties")
    void createDefaultPostgresConfig() {
        var props = new ConnectorProperties();
        props.getPostgres().setHost("pg-host");
        props.getPostgres().setPort(9999);
        props.getPostgres().setDatabase("mydb");

        var factory = new ConnectorFactory(sparkSessionProvider, props);
        var config = factory.createDefaultPostgresConfig("default-pg");

        assertEquals("pg-host", config.host());
        assertEquals(9999, config.port());
        assertEquals("mydb", config.database());
        assertEquals("default-pg", config.datasourceName());
    }

    @Test
    @DisplayName("createDefaultCsvConfig populates from ConnectorProperties")
    void createDefaultCsvConfig() {
        var props = new ConnectorProperties();
        props.getCsv().setDelimiter('|');
        props.getCsv().setHeader(false);

        var factory = new ConnectorFactory(sparkSessionProvider, props);
        var config = factory.createDefaultCsvConfig("/data/file.csv", "default-csv");

        assertEquals("/data/file.csv", config.filePath());
        assertEquals('|', config.delimiter());
        assertFalse(config.header());
        assertEquals("default-csv", config.datasourceName());
    }

    @Test
    @DisplayName("getConnectorProperties returns the injected properties")
    void getConnectorProperties() {
        var props = new ConnectorProperties();
        var factory = new ConnectorFactory(sparkSessionProvider, props);

        assertSame(props, factory.getConnectorProperties());
    }

    @Test
    @DisplayName("Single-arg constructor creates default ConnectorProperties")
    void singleArgConstructor() {
        var factory = new ConnectorFactory(sparkSessionProvider);

        assertNotNull(factory.getConnectorProperties());
        assertNotNull(factory.getConnectorProperties().getPostgres());
        assertNotNull(factory.getConnectorProperties().getCsv());
    }
}
