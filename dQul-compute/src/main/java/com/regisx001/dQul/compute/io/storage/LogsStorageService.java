package com.regisx001.dQul.compute.io.storage;

import com.regisx001.dQul.compute.dto.logs.LogsAggregationResultDto;

/**
 * Abstraction for persisting batch logs aggregation results to persistent storage (MinIO/S3).
 */
public interface LogsStorageService {

    /**
     * Persists the computed LogsAggregationResultDto to MinIO/S3.
     * The output filename MUST be composed of a UUID and a timestamp.
     *
     * @param resultDto the aggregated logs result
     * @return the destination S3 URI (e.g. s3a://bucket/logs-aggregations/{uuid}_{timestamp}.json)
     */
    String saveLogsAggregationResult(LogsAggregationResultDto resultDto);
}
