package com.regisx001.dQul.logs.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Trace-level analytics derived from {@code traceId} grouping: unique traces,
 * per-trace event counts, trace duration (first-last timestamp span), and
 * failure rate.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraceAnalyticsDto {

    private long uniqueTraces;
    private double averageLogsPerTrace;
    private double averageDurationMs;
    private double medianDurationMs;
    private double p95DurationMs;
    private double p99DurationMs;
    private long failedTraces;
    private double traceErrorRatePercentage;
}
