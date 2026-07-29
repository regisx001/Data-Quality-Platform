package com.regisx001.dQul.connector.api;

/**
 * Combined connector interface that exposes both {@link MetadataApi} and
 * {@link ComputeApi}.
 */
public interface DataSourceConnector extends MetadataApi, ComputeApi {

    // Combines both APIs — no additional methods.
}
