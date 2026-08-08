package com.regisx001.dQul.logs.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per endpoint (httpMethod + path) rollup within the analyzed window.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointAnalyticsDto {

    private String httpMethod;
    private String path;
    private long requestCount;
    private long errorCount;
    private double errorRatePercentage;
    private double averageLatencyMs;
    private double p95LatencyMs;
    private double p99LatencyMs;
    private double maxLatencyMs;
}
