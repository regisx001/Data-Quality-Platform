package com.regisx001.dQul.logs.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Top-level envelope returned by the log analytics endpoint. Aggregates a
 * bounded time window of {@code LogEntry} rows across the fixed observability
 * dimensions (volume, log level, service, category, HTTP, latency, error
 * signature, trace, user). Subsystem-specific dimensions continue to live in
 * {@code metadata} and are out of scope for the universal model.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogAnalyticsDto {

    /** Window actually analyzed (aligned to granularity where relevant). */
    private Instant from;
    private Instant to;
    private long totalLogs;

    private VolumeAnalyticsDto volume;
    private LevelAnalyticsDto levels;
    private List<ServiceAnalyticsDto> services;
    private List<CategoryAnalyticsDto> categories;
    private HttpAnalyticsDto http;
    private LatencyAnalyticsDto latency;
    private List<ErrorSignatureDto> errorSignatures;
    private TraceAnalyticsDto traces;
    private Map<String, UserAnalyticsDto> users;
}
