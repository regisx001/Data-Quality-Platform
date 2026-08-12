package com.regisx001.dQul.compute.service.impl;

import com.regisx001.dQul.compute.dto.DatasetProfileRequest;
import com.regisx001.dQul.compute.dto.logs.LogsAggregationCompletedEvent;
import com.regisx001.dQul.compute.dto.logs.LogsAggregationRequest;
import com.regisx001.dQul.compute.dto.logs.LogsAggregationResultDto;
import com.regisx001.dQul.compute.engine.batch.logs.LogsAggregatorEngine;
import com.regisx001.dQul.compute.io.reader.DatasetReader;
import com.regisx001.dQul.compute.io.storage.LogsStorageService;
import com.regisx001.dQul.compute.service.LogsJobOrchestratorService;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LogsJobOrchestratorServiceImpl implements LogsJobOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(LogsJobOrchestratorServiceImpl.class);

    private final DatasetReader datasetReader;
    private final LogsAggregatorEngine logsAggregatorEngine;
    private final LogsStorageService logsStorageService;

    @Value("${dqul.logs.datasource.url:jdbc:postgresql://postgres:5432/dqul_logs}")
    private String logsDbUrl;

    @Value("${dqul.logs.datasource.username:postgres}")
    private String logsDbUsername;

    @Value("${dqul.logs.datasource.password:postgres}")
    private String logsDbPassword;

    @Value("${dqul.logs.datasource.driver-class-name:org.postgresql.Driver}")
    private String logsDbDriver;

    public LogsJobOrchestratorServiceImpl(DatasetReader datasetReader,
                                          LogsAggregatorEngine logsAggregatorEngine,
                                          LogsStorageService logsStorageService) {
        this.datasetReader = datasetReader;
        this.logsAggregatorEngine = logsAggregatorEngine;
        this.logsStorageService = logsStorageService;
    }

    @Override
    public LogsAggregationCompletedEvent executeLogsAggregationJob(LogsAggregationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("LogsAggregationRequest must not be null");
        }

        log.info("Orchestrating batch logs aggregation job for jobId={}", request.getJobId());
        long startTime = System.currentTimeMillis();

        try {
            // Configure DatasetProfileRequest pointing directly to configured dqul_logs database log_entries table
            DatasetProfileRequest readerRequest = DatasetProfileRequest.builder()
                    .profileId(request.getJobId())
                    .sourceType("POSTGRES_JDBC")
                    .jdbcConfig(DatasetProfileRequest.JdbcConfig.builder()
                            .url(logsDbUrl)
                            .user(logsDbUsername)
                            .password(logsDbPassword)
                            .dbtable("log_entries")
                            .driver(logsDbDriver)
                            .build())
                    .build();

            // 1. Read logs dataset into Spark DataFrame via DatasetReader
            Dataset<Row> df = datasetReader.read(readerRequest);

            // 2. Perform Spark batch logs aggregation via LogsAggregatorEngine
            LogsAggregationResultDto resultDto = logsAggregatorEngine.aggregate(request, df);

            // 3. Persist result JSON to MinIO/S3 via LogsStorageService ({uuid}_{timestamp}.json)
            String s3ResultUri = logsStorageService.saveLogsAggregationResult(resultDto);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Successfully completed batch logs aggregation job for jobId={} in {} ms", request.getJobId(), duration);

            return LogsAggregationCompletedEvent.builder()
                    .jobId(request.getJobId())
                    .status("SUCCESS")
                    .s3ResultUri(s3ResultUri)
                    .totalLogsCount(resultDto.getTotalLogsCount())
                    .executionDurationMs(duration)
                    .completedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to execute batch logs aggregation job for jobId={}: {}", request.getJobId(), e.getMessage(), e);

            return LogsAggregationCompletedEvent.builder()
                    .jobId(request.getJobId())
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .executionDurationMs(duration)
                    .completedAt(LocalDateTime.now())
                    .build();
        }
    }
}
