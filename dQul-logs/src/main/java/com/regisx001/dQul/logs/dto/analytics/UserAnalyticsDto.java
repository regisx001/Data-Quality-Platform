package com.regisx001.dQul.logs.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-user analytics derived from {@code userId}: activity volume, error rate
 * and latency percentiles for a single user within the analyzed window.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAnalyticsDto {

    private String userId;
    private String userEmail;
    private long totalLogs;
    private long errorCount;
    private double errorRatePercentage;
    private double averageLatencyMs;
    private double p95LatencyMs;
}
