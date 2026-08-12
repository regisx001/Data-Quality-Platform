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
public class LogsAggregationRequest {

    private UUID jobId;
    private String from;
    private String to;

    @Builder.Default
    private LocalDateTime requestedAt = LocalDateTime.now();
}
