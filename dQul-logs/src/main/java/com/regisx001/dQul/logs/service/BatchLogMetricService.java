package com.regisx001.dQul.logs.service;

import com.regisx001.dQul.logs.dto.batch.BatchLogMetricDto;
import com.regisx001.dQul.logs.dto.batch.LogsAggregationCompletedEvent;

import java.util.List;
import java.util.UUID;

public interface BatchLogMetricService {

    BatchLogMetricDto createPendingBatchMetric(UUID jobId, String from, String to);

    BatchLogMetricDto saveBatchMetric(LogsAggregationCompletedEvent event);

    List<BatchLogMetricDto> getRecentBatchMetrics(int limit);
}
