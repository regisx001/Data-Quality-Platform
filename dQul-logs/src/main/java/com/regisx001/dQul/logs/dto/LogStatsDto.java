package com.regisx001.dQul.logs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogStatsDto {
    private long totalLogs;
    private long errorCount;
    private long warnCount;
    private long infoCount;
    private double errorRatePercentage;
    private double averageLatencyMs;
    private Map<String, Long> logsByService;
    private Map<String, Long> logsByCategory;
}
