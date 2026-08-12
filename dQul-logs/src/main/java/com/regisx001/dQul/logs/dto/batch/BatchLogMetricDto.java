package com.regisx001.dQul.logs.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchLogMetricDto {

    private UUID id;
    private UUID jobId;
    private String status;
    private String fromTimestamp;
    private String toTimestamp;
    private Long totalLogsCount;
    private Double avgExecutionTimeMs;
    private String minioStoragePath;
    private LogsAggregationResultDto resultData;
    private Instant createdAt;
}
