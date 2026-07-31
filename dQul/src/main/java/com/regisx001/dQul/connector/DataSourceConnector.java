package com.regisx001.dQul.connector;

import java.util.List;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import com.regisx001.dQul.connector.api.ColumnMetadata;
import com.regisx001.dQul.connector.api.ConnectionTestResult;
import com.regisx001.dQul.connector.api.DatasetDescriptor;
import com.regisx001.dQul.connector.api.DatasetMetadata;

/**
 * Combined connector interface for data source connectivity.
 *
 * <p>
 * Implementations provide metadata discovery, connection testing,
 * and the ability to create Spark {@link Dataset DataFrames} from
 * a datasource.
 */
public interface DataSourceConnector {

    /** Tests the connection to the datasource. */
    ConnectionTestResult testConnection();

    /** Enumerates all datasets exposed by this datasource. */
    List<DatasetDescriptor> discoverDatasets();

    /** Retrieves detailed schema metadata for a specific dataset. */
    DatasetMetadata getMetadata(String datasetId);

    /**
     * Creates a {@link DataReader reader} that the Spark compute engine
     * can use to read the data of a specific dataset.
     */
    DataReader createReader(String datasetId);

    // ──────────────────────────────────────────────
    // Nested reader type
    // ──────────────────────────────────────────────

    /**
     * Abstraction that produces a Spark DataFrame from a connector-backed source.
     */
    @FunctionalInterface
    interface DataReader {
        /** Reads the dataset into a Spark {@link Dataset<Row>}. */
        Dataset<Row> read();
    }
}
