package com.regisx001.dQul.compute.dto.logs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogsAggregationResultDto {

    private UUID jobId;
    private Long totalLogsCount;
    private Map<String, Long> levelCounts;
    private Map<String, Long> serviceCounts;
    private Map<String, Long> categoryCounts;
    private Double avgExecutionTimeMs;
    private Long maxExecutionTimeMs;
    private List<ErrorLogSummaryDto> topErrorMessages;
    private String minTimestamp;
    private String maxTimestamp;
    private LocalDateTime aggregatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorLogSummaryDto {
        private String serviceName;
        private String category;
        private String message;
        private Long count;
    }
}
