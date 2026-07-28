package com.regisx001.dQul.connector.api;

/**
 * Compute API of a datasource connector.
 *
 * <p>
 * Produces a Spark {@link DataReader DataFrame} for a given dataset,
 * allowing the Validation Engine and profiling pipelines to process data
 * without knowing the physical location or storage technology.
 *
 * <p>
 * This is the only face that the compute layer depends on. Metadata
 * exploration (discovery, schema, statistics) belongs to {@link MetadataApi}.
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
