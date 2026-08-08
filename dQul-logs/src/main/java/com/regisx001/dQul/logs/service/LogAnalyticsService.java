package com.regisx001.dQul.logs.service;

import com.regisx001.dQul.logs.dto.analytics.LogAnalyticsDto;
import com.regisx001.dQul.logs.dto.analytics.LogAnalyticsRequest;

/**
 * Aggregates the raw {@code LogEntry} store into a structured analytics model
 * across the universal observability dimensions (volume, log level, service,
 * category, HTTP, latency, error signature, trace, user).
 *
 * <p>Implementations are expected to be pure read-only aggregations over a time
 * window and safe to cache. The endpoint contract is deliberately small so
 * subsystem-specific dimensions can be layered on top via {@code metadata}
 * without altering this interface.
 */
public interface LogAnalyticsService {

    /**
     * Computes analytics for the given window and optional filters.
     *
     * @param request window bounds and filters; from/to are inclusive, nulls
     *                treated as unlimited, granularity defaults to hourly.
     * @return aggregated analytics envelope
     */
    LogAnalyticsDto analyze(LogAnalyticsRequest request);
}
