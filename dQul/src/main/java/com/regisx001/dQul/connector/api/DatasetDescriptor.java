package com.regisx001.dQul.connector.api;

/**
 * Lightweight descriptor that identifies a single dataset exposed by a datasource.
 */
public record DatasetDescriptor(
        String id,
        String name,
        DatasetType type,
        String description,
        Long rowCount) {

    public DatasetDescriptor(String id, String name, DatasetType type) {
        this(id, name, type, null, null);
    }

    public DatasetDescriptor(String id, String name, DatasetType type, String description) {
        this(id, name, type, description, null);
    }
}
