package com.regisx001.dQul.logs.service;

import com.regisx001.dQul.logs.dto.analytics.LogAnalyticsDto;
import com.regisx001.dQul.logs.dto.analytics.LogAnalyticsRequest;

/**
 * Aggregates the raw {@code LogEntry} store into a structured analytics model
 * across the universal observability dimensions.
 *
 * @deprecated Legacy aggregation interface. Replaced by Spark Structured Streaming
 *             (RealtimeLogSseService) and Spark Batch Aggregations (BatchLogMetricService).
 */
@Deprecated(since = "2.0", forRemoval = false)
public interface LogAnalyticsService {

    /**
     * Computes analytics for the given window and optional filters.
     *
     * @param request window bounds and filters
     * @return aggregated analytics envelope
     */
    @Deprecated(since = "2.0", forRemoval = false)
    LogAnalyticsDto analyze(LogAnalyticsRequest request);
}
