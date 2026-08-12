package com.regisx001.dQul.compute.dto.logs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogsAggregationCompletedEvent {

    private UUID jobId;
    private String status; // "SUCCESS", "FAILED"
    private String s3ResultUri;
    private Long totalLogsCount;
    private Long executionDurationMs;
    private String errorMessage;
    private LocalDateTime completedAt;
}
