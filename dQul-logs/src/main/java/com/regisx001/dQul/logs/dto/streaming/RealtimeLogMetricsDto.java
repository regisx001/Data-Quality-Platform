package com.regisx001.dQul.logs.dto.streaming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RealtimeLogMetricsDto {

    private String windowStart;
    private String windowEnd;
    private Double throughputLogsPerSec;
    private Long totalLogsCount;
    private Long infoCount;
    private Long warnCount;
    private Long errorCount;
    private Long debugCount;
    private Map<String, Long> levelCounts;
    private Map<String, Long> serviceCounts;
    private Double avgExecutionTimeMs;
    private Instant timestamp;
}
