package com.regisx001.dQul.compute.service;

import com.regisx001.dQul.compute.dto.DatasetProfileRequest;
import com.regisx001.dQul.compute.dto.logs.LogsAggregationCompletedEvent;
import com.regisx001.dQul.compute.dto.logs.LogsAggregationRequest;
import com.regisx001.dQul.compute.dto.logs.LogsAggregationResultDto;
import com.regisx001.dQul.compute.engine.batch.logs.LogsAggregatorEngine;
import com.regisx001.dQul.compute.io.reader.DatasetReader;
import com.regisx001.dQul.compute.io.storage.LogsStorageService;
import com.regisx001.dQul.compute.service.impl.LogsJobOrchestratorServiceImpl;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogsJobOrchestratorServiceImplTest {

    @Mock
    private DatasetReader datasetReader;

    @Mock
    private LogsAggregatorEngine logsAggregatorEngine;

    @Mock
    private LogsStorageService logsStorageService;

    @Mock
    private Dataset<Row> dataset;

    private LogsJobOrchestratorServiceImpl orchestratorService;

    @BeforeEach
    void setUp() {
        orchestratorService = new LogsJobOrchestratorServiceImpl(
                datasetReader, logsAggregatorEngine, logsStorageService
        );
    }

    @Test
    void executeLogsAggregationJob_nullRequest_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> orchestratorService.executeLogsAggregationJob(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void executeLogsAggregationJob_successFlow() {
        UUID jobId = UUID.randomUUID();
        LogsAggregationRequest request = LogsAggregationRequest.builder()
                .jobId(jobId)
                .from("2026-08-01T00:00:00Z")
                .to("2026-08-10T00:00:00Z")
                .build();

        LogsAggregationResultDto resultDto = LogsAggregationResultDto.builder()
                .jobId(jobId)
                .totalLogsCount(1000L)
                .levelCounts(Map.of("INFO", 700L, "ERROR", 300L))
                .avgExecutionTimeMs(150.0)
                .build();

        given(datasetReader.read(any(DatasetProfileRequest.class))).willReturn(dataset);
        given(logsAggregatorEngine.aggregate(request, dataset)).willReturn(resultDto);
        given(logsStorageService.saveLogsAggregationResult(resultDto)).willReturn("s3://dqul-results/" + jobId + ".json");

        LogsAggregationCompletedEvent event = orchestratorService.executeLogsAggregationJob(request);

        assertThat(event).isNotNull();
        assertThat(event.getJobId()).isEqualTo(jobId);
        assertThat(event.getStatus()).isEqualTo("SUCCESS");
        assertThat(event.getS3ResultUri()).isEqualTo("s3://dqul-results/" + jobId + ".json");
        assertThat(event.getResultData()).isEqualTo(resultDto);

        verify(datasetReader).read(any(DatasetProfileRequest.class));
        verify(logsAggregatorEngine).aggregate(request, dataset);
        verify(logsStorageService).saveLogsAggregationResult(resultDto);
    }

    @Test
    void executeLogsAggregationJob_exceptionOccurs_returnsFailedEvent() {
        UUID jobId = UUID.randomUUID();
        LogsAggregationRequest request = LogsAggregationRequest.builder()
                .jobId(jobId)
                .build();

        given(datasetReader.read(any(DatasetProfileRequest.class)))
                .willThrow(new RuntimeException("Database connection timeout"));

        LogsAggregationCompletedEvent event = orchestratorService.executeLogsAggregationJob(request);

        assertThat(event).isNotNull();
        assertThat(event.getJobId()).isEqualTo(jobId);
        assertThat(event.getStatus()).isEqualTo("FAILED");
        assertThat(event.getErrorMessage()).contains("Database connection timeout");
    }
}
