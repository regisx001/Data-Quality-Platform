package com.regisx001.dQul.logs.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Log volume analytics for the analyzed window: total count plus a
 * granularity-bucketed time series and per-bucket extremes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolumeAnalyticsDto {

    private double logsPerSecond;
    private double logsPerMinute;
    private long maxLogsInBucket;
    private long minLogsInBucket;

    /**
     * Ordered buckets: each holds the bucket label (e.g. "2026-08-08T10:00:00Z")
     * and count.
     */
    private List<VolumeBucket> timeSeries;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VolumeBucket {
        private String bucket;
        private long count;
    }
}
