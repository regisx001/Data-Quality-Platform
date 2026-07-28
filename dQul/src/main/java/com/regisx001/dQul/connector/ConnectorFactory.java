package com.regisx001.dQul.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.regisx001.dQul.compute.spark.SparkSessionProvider;
import com.regisx001.dQul.connector.api.ConnectorConfig;
import com.regisx001.dQul.connector.api.CsvConnectorConfig;
import com.regisx001.dQul.connector.api.DataSourceConnector;
import com.regisx001.dQul.connector.api.PostgresConnectorConfig;
import com.regisx001.dQul.connector.csv.CsvDataSourceConnector;
import com.regisx001.dQul.connector.postgres.PostgresDataSourceConnector;

/**
 * Factory for creating {@link DataSourceConnector} instances based on the
 * provided {@link ConnectorConfig}.
 *
 * <p>
 * Each connector exposes <b>two faces</b>:
 * <ul>
 * <li>{@link com.regisx001.dQul.connector.api.MetadataApi MetadataApi}
 * &mdash; native-driver metadata exploration (used by UI &amp;
 * metadata services)</li>
 * <li>{@link com.regisx001.dQul.connector.api.ComputeApi ComputeApi}
 * &mdash; Spark DataFrame production (used by Validation Engine
 * &amp; profiling pipelines)</li>
 * </ul>
 *
 * <p>
 * Consumers should depend on the specific interface they need rather than
 * on {@link DataSourceConnector} directly, to keep dependency boundaries
 * clean.
 *
 * <p>
 * To support a new datasource type:
 * <ol>
 * <li>Create a configuration record implementing {@link ConnectorConfig}</li>
 * <li>Create a connector class implementing {@code MetadataApi} and
 * {@code ComputeApi}</li>
 * <li>Add the new config type to the {@code permits} clause of
 * {@code ConnectorConfig}</li>
 * <li>Add a new {@code case} to the {@code createConnector} switch</li>
 * </ol>
 */
@Component
public class ConnectorFactory {

    private static final Logger log = LoggerFactory.getLogger(ConnectorFactory.class);

    private final SparkSessionProvider sparkSessionProvider;

    public ConnectorFactory(SparkSessionProvider sparkSessionProvider) {
        this.sparkSessionProvider = sparkSessionProvider;
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
            case PostgresConnectorConfig c -> new PostgresDataSourceConnector(c, sparkSessionProvider);
            case CsvConnectorConfig c -> new CsvDataSourceConnector(c, sparkSessionProvider);
        };
    }
}
