package com.regisx001.dQul.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.regisx001.dQul.compute.spark.SparkSessionProvider;
import com.regisx001.dQul.connector.csv.CsvDataSourceConnector;
import com.regisx001.dQul.connector.postgres.PostgresDataSourceConnector;

/**
 * Factory for creating {@link DataSourceConnector} instances based on the
 * provided {@link ConnectorConfig}.
 */
@Component
public class ConnectorFactory {

    private static final Logger log = LoggerFactory.getLogger(ConnectorFactory.class);

    private final SparkSessionProvider sparkSessionProvider;
    private final ConnectorProperties connectorProperties;

    public ConnectorFactory(SparkSessionProvider sparkSessionProvider) {
        this(sparkSessionProvider, new ConnectorProperties());
    }

    @Autowired
    public ConnectorFactory(SparkSessionProvider sparkSessionProvider, ConnectorProperties connectorProperties) {
        this.sparkSessionProvider = sparkSessionProvider;
        this.connectorProperties = connectorProperties != null ? connectorProperties : new ConnectorProperties();
    }

    /**
     * Creates a default {@link ConnectorConfig.Postgres} using configuration
     * properties.
     *
     * @param datasourceName human-readable name
     * @return Postgres config populated from application properties
     */
    public ConnectorConfig.Postgres createDefaultPostgresConfig(String datasourceName) {
        ConnectorProperties.Postgres p = connectorProperties.getPostgres();
        return new ConnectorConfig.Postgres(
                p.getHost(), p.getPort(), p.getDatabase(), p.getSchema(),
                p.getUsername(), p.getPassword(), p.isSsl(), p.getConnectionTimeoutMs(),
                p.getFetchSize(), datasourceName);
    }

    /**
     * Creates a default {@link ConnectorConfig.Csv} using configuration properties.
     *
     * @param filePath       path to the CSV file
     * @param datasourceName human-readable name
     * @return Csv config populated from application properties
     */
    public ConnectorConfig.Csv createDefaultCsvConfig(String filePath, String datasourceName) {
        ConnectorProperties.Csv c = connectorProperties.getCsv();
        return new ConnectorConfig.Csv(
                filePath, c.getDelimiter(), c.isHeader(), c.getEncoding(),
                c.getQuoteChar(), c.getEscapeChar(), c.isInferSchema(), datasourceName);
    }

    /**
     * Creates the appropriate {@link DataSourceConnector} for the given
     * configuration.
     *
     * @param config the connector configuration (must be a known subtype)
     * @return a connector ready to interact with the datasource
     * @throws IllegalArgumentException if the config type is unknown
     */
    public DataSourceConnector createConnector(ConnectorConfig config) {
        log.info("Creating connector for datasource: {}", config.datasourceName());

        return switch (config) {
            case ConnectorConfig.Postgres c -> new PostgresDataSourceConnector(c, sparkSessionProvider);
            case ConnectorConfig.Csv c -> new CsvDataSourceConnector(c, sparkSessionProvider);
        };
    }

    public ConnectorProperties getConnectorProperties() {
        return connectorProperties;
    }
}
