package com.regisx001.dQul.logs.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Latency analytics for numeric {@code executionTimeMs} within the analyzed
 * window: central tendency, spread, percentiles and distribution.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LatencyAnalyticsDto {

    private long sampleCount;
    private double averageMs;
    private double medianMs;
    private double minMs;
    private double maxMs;
    private double p50Ms;
    private double p75Ms;
    private double p90Ms;
    private double p95Ms;
    private double p99Ms;
    private double p999Ms;
    private double standardDeviationMs;
    private double varianceMs2;
    private long slowRequestCount;
    private double slowRequestPercentage;
}
