package com.regisx001.dQul.compute.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComputeJobRequest {
    private String jobId;
    private String jobType; // e.g. "PROFILING", "AGGREGATION", "CUSTOM_SQL"
    private String datasetPath;
    private String sqlQuery;
    private Map<String, String> options;
}
