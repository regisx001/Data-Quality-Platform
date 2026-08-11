package com.regisx001.dQul.compute.dto;

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
public class DatasetProfileCompletedEvent {

    private UUID profileId;
    private UUID datasetId;

    /**
     * Status of profiling job: SUCCESS, FAILED
     */
    private String status;

    /**
     * S3 URI of the written single JSON profile document
     */
    private String s3ResultUri;

    private Long rowCount;
    private Integer columnCount;
    private Long executionDurationMs;

    private String errorMessage;

    @Builder.Default
    private LocalDateTime completedAt = LocalDateTime.now();
}
