package com.regisx001.dQul.connector.api;

/**
 * Compute API of a datasource connector.
 *
 * @see MetadataApi
 * @see DataSourceConnector
 */
@FunctionalInterface
public interface ComputeApi {

    /**
     * Creates a {@link DataReader} that the Spark compute engine can use
     * to read the data of a specific dataset.
     *
     * @param datasetId the unique identifier of the dataset
     * @return a reader capable of producing a Spark DataFrame
     */
    DataReader createReader(String datasetId);
}
