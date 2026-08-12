package com.regisx001.dQul.compute.service;

import com.regisx001.dQul.compute.dto.DatasetProfileCompletedEvent;
import com.regisx001.dQul.compute.dto.DatasetProfileRequest;

/**
 * Service orchestrator responsible for coordinating profiling job lifecycle:
 * reading data, running profiler engine, storing output JSON, and constructing completion event.
 */
public interface ProfileJobOrchestratorService {

    /**
     * Executes the end-to-end dataset profiling job workflow.
     *
     * @param request dataset profile request details
     * @return DatasetProfileCompletedEvent with job status and result S3 URI
     */
    DatasetProfileCompletedEvent executeProfilingJob(DatasetProfileRequest request);
}
