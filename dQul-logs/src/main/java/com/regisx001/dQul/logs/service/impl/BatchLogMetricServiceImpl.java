package com.regisx001.dQul.logs.service.impl;

import com.regisx001.dQul.logs.domain.BatchLogMetricEntity;
import com.regisx001.dQul.logs.dto.batch.BatchLogMetricDto;
import com.regisx001.dQul.logs.dto.batch.LogsAggregationCompletedEvent;
import com.regisx001.dQul.logs.dto.batch.LogsAggregationResultDto;
import com.regisx001.dQul.logs.repository.BatchLogMetricRepository;
import com.regisx001.dQul.logs.service.BatchLogMetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchLogMetricServiceImpl implements BatchLogMetricService {

    private final BatchLogMetricRepository repository;

    @Override
    @Transactional
    public BatchLogMetricDto createPendingBatchMetric(UUID jobId, String from, String to) {
        if (jobId == null) {
            throw new IllegalArgumentException("jobId must not be null");
        }

        log.info("Creating PENDING batch log metric record in PostgreSQL for jobId={}", jobId);

        BatchLogMetricEntity entity = BatchLogMetricEntity.builder()
                .jobId(jobId)
                .status("PENDING")
                .fromTimestamp(from)
                .toTimestamp(to)
                .totalLogsCount(0L)
                .createdAt(Instant.now())
                .build();

        BatchLogMetricEntity saved = repository.save(entity);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public BatchLogMetricDto saveBatchMetric(LogsAggregationCompletedEvent event) {
        if (event == null || event.getJobId() == null) {
            log.warn("Received null event or null jobId for batch log metric persistence; skipping.");
            return null;
        }

        log.info("Persisting completed Spark batch analytics result for jobId={}", event.getJobId());

        Instant createdAt = event.getCompletedAt() != null
                ? event.getCompletedAt().toInstant(ZoneOffset.UTC)
                : Instant.now();

        LogsAggregationResultDto resData = event.getResultData();
        Long totalLogsCount = resData != null && resData.getTotalLogsCount() != null ? resData.getTotalLogsCount() : 0L;
        Double avgLatency = resData != null ? resData.getAvgExecutionTimeMs() : null;

        Optional<BatchLogMetricEntity> existingOpt = repository.findByJobId(event.getJobId());
        BatchLogMetricEntity entity;

        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
            entity.setStatus(event.getStatus() != null ? event.getStatus() : "SUCCESS");
            entity.setTotalLogsCount(totalLogsCount);
            entity.setAvgExecutionTimeMs(avgLatency);
            entity.setMinioStoragePath(event.getS3ResultUri());
            entity.setResultData(resData);
        } else {
            entity = BatchLogMetricEntity.builder()
                    .jobId(event.getJobId())
                    .status(event.getStatus() != null ? event.getStatus() : "SUCCESS")
                    .fromTimestamp(event.getFrom())
                    .toTimestamp(event.getTo())
                    .totalLogsCount(totalLogsCount)
                    .avgExecutionTimeMs(avgLatency)
                    .minioStoragePath(event.getS3ResultUri())
                    .resultData(resData)
                    .createdAt(createdAt)
                    .build();
        }

        BatchLogMetricEntity saved = repository.save(entity);
        log.info("Successfully saved batch_log_metric record id={} for jobId={}", saved.getId(), saved.getJobId());
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchLogMetricDto> getRecentBatchMetrics(int limit) {
        int maxLimit = Math.min(Math.max(limit, 1), 200);
        List<BatchLogMetricEntity> entities = repository.findRecentBatchMetrics(PageRequest.of(0, maxLimit));
        return entities.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    private BatchLogMetricDto mapToDto(BatchLogMetricEntity entity) {
        return BatchLogMetricDto.builder()
                .id(entity.getId())
                .jobId(entity.getJobId())
                .status(entity.getStatus())
                .fromTimestamp(entity.getFromTimestamp())
                .toTimestamp(entity.getToTimestamp())
                .totalLogsCount(entity.getTotalLogsCount())
                .avgExecutionTimeMs(entity.getAvgExecutionTimeMs())
                .minioStoragePath(entity.getMinioStoragePath())
                .resultData(entity.getResultData())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
