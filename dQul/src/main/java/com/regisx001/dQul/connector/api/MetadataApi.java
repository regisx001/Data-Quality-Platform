package com.regisx001.dQul.connector.api;

import java.util.List;

/**
 * Metadata API of a datasource connector.
 */
public interface MetadataApi {

    /**
     * Tests the connection to the datasource using the provided credentials.
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
