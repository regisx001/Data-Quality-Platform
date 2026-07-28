package com.regisx001.dQul.connector.api;

/**
 * Combined connector interface that exposes both {@link MetadataApi} and
 * {@link ComputeApi}.
 *
 * <p>
 * A concrete connector implementation provides <b>two faces</b>:
 * <ul>
 * <li><b>Metadata API</b> — native-driver-based operations for
 * connection testing, dataset discovery, schema extraction, and
 * statistics. Used by the UI and metadata services.</li>
 * <li><b>Compute API</b> — produces a Spark {@link DataReader DataFrame}
 * for a dataset. Used by the Validation Engine and profiling
 * pipelines.</li>
 * </ul>
 *
 * <p>
 * Consumers should depend on the specific interface they need
 * ({@link MetadataApi} or {@link ComputeApi}) rather than this combined
 * type, to keep dependency boundaries clean. This combined interface is
 * primarily used by the {@code ConnectorFactory} and by implementations.
 *
 * @see MetadataApi
 * @see ComputeApi
 */
public interface DataSourceConnector extends MetadataApi, ComputeApi {

    // Combines both APIs — no additional methods.
}