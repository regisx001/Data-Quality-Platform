package com.regisx001.dQul.logs.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-category analytics: volume share, severity mix and latency percentiles
 * for a single category within the analyzed window.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryAnalyticsDto {

    private String category;
    private long totalLogs;
    private double logSharePercentage;
    private long errorCount;
    private long fatalCount;
    private double errorRatePercentage;
    private double averageLatencyMs;
    private double p95LatencyMs;
    private double p99LatencyMs;
}
