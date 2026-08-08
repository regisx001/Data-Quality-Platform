package com.regisx001.dQul.logs.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-service analytics: volume, severity counts, error rate and latency
 * percentiles for a single service within the analyzed window.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceAnalyticsDto {

    private String serviceName;
    private long totalLogs;
    private double logSharePercentage;
    private long errorCount;
    private long fatalCount;
    private long warnCount;
    private double errorRatePercentage;
    private double averageLatencyMs;
    private double p95LatencyMs;
    private double p99LatencyMs;
    private double maxLatencyMs;
}
