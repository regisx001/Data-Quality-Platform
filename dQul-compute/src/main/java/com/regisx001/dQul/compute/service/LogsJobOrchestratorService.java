package com.regisx001.dQul.compute.service;

import com.regisx001.dQul.compute.dto.logs.LogsAggregationCompletedEvent;
import com.regisx001.dQul.compute.dto.logs.LogsAggregationRequest;

/**
 * Service orchestrator responsible for coordinating batch logs aggregation jobs.
 */
public interface LogsJobOrchestratorService {

    /**
     * Executes the end-to-end batch logs aggregation job workflow.
     *
     * @param request logs aggregation request details
     * @return LogsAggregationCompletedEvent with status and result S3 URI
     */
    LogsAggregationCompletedEvent executeLogsAggregationJob(LogsAggregationRequest request);
}
