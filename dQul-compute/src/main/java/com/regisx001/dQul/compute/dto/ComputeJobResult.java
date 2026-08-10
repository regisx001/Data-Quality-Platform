package com.regisx001.dQul.compute.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComputeJobResult {
    private String jobId;
    private String status; // "SUCCESS", "FAILED"
    private long totalRows;
    private long executionTimeMs;
    private Instant completedAt;
    private String errorMessage;
    private List<Map<String, Object>> previewRows;
    private Map<String, Object> metrics;
}
