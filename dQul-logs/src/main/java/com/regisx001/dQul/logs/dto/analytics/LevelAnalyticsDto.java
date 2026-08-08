package com.regisx001.dQul.logs.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Log-level distribution analytics: count and percentage for each severity,
 * plus aggregate ratios that operators track (error rate, warn+ rate, etc).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LevelAnalyticsDto {

    private long infoCount;
    private long debugCount;
    private long traceCount;
    private long warnCount;
    private long errorCount;
    private long fatalCount;

    private double infoRatio;
    private double debugRatio;
    private double traceRatio;
    private double warnRatio;
    private double errorRatio;
    private double fatalRatio;

    /** Combined (error + fatal) / total, expressed as a percentage (0-100). */
    private double errorRatePercentage;

    /**
     * Combined (warn + error + fatal) / total, expressed as a percentage (0-100).
     */
    private double warnPlusRatePercentage;

    private List<LevelDistribution> distribution;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LevelDistribution {
        private String level;
        private long count;
        private double percentage;
    }
}
