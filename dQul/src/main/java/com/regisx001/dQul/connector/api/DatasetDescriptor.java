package com.regisx001.dQul.connector.api;

/**
 * Lightweight descriptor that identifies a single dataset exposed by a
 * datasource.
 * This is the discovery-level representation; detailed schema information is
 * obtained via {@link DataSourceConnector#getMetadata(String)}.
 *
 * @param id          A unique identifier for the dataset within its datasource
 * @param name        A human-readable name (table name, file name, collection
 *                    name, etc.)
 * @param type        The {@link DatasetType} categorisation
 * @param description An optional description of the dataset's contents
 */
public record DatasetDescriptor(
        String id,
        String name,
        DatasetType type,
        String description) {

    public DatasetDescriptor(String id, String name, DatasetType type) {
        this(id, name, type, null);
    }
}