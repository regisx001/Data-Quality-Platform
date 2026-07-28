package com.regisx001.dQul.connector.api;

import java.util.List;

/**
 * Metadata API of a datasource connector.
 *
 * <p>
 * Uses <b>native drivers</b> (JDBC, MongoDB driver, filesystem APIs, cloud
 * SDKs, etc.) for rich metadata exploration. This is the face that the UI
 * and metadata services depend on.
 *
 * <p>
 * Responsibilities:
 * <ul>
 * <li>Testing connectivity with the provided credentials</li>
 * <li>Discovering available datasets (tables, views, files, collections)</li>
 * <li>Reading schemas, primary/foreign keys, indexes, statistics</li>
 * </ul>
 *
 * <p>
 * The Validation Engine and compute pipelines do <b>not</b> use this
 * interface — they depend on {@link ComputeApi} instead.
 *
 * @see ComputeApi
 * @see DataSourceConnector
 */
public interface MetadataApi {

    /**
     * Tests the connection to the datasource using the provided credentials
     * and configuration.
     *
     * @return the connection test result
     */
    ConnectionTestResult testConnection();

    /**
     * Enumerates all datasets exposed by this datasource.
     *
     * @return a list of lightweight dataset descriptors
     */
    List<DatasetDescriptor> discoverDatasets();

    /**
     * Retrieves detailed schema metadata for a specific dataset.
     *
     * @param datasetId the unique identifier of the dataset
     * @return the full metadata including column definitions
     */
    DatasetMetadata getMetadata(String datasetId);
}
