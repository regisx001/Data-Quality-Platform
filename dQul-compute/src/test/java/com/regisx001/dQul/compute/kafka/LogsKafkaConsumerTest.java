package com.regisx001.dQul.compute.kafka;

import com.regisx001.dQul.compute.dto.logs.LogsAggregationCompletedEvent;
import com.regisx001.dQul.compute.dto.logs.LogsAggregationRequest;
import com.regisx001.dQul.compute.service.LogsJobOrchestratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogsKafkaConsumerTest {

    @Mock
    private LogsJobOrchestratorService orchestratorService;

    @Mock
    private LogsKafkaProducer kafkaProducer;

    private LogsKafkaConsumer kafkaConsumer;

    @BeforeEach
    void setUp() {
        kafkaConsumer = new LogsKafkaConsumer(orchestratorService, kafkaProducer);
    }

    @Test
    void consumeLogsAggregationRequest_nullRequest_shouldIgnore() {
        kafkaConsumer.consumeLogsAggregationRequest(null);

        verify(orchestratorService, never()).executeLogsAggregationJob(any());
        verify(kafkaProducer, never()).sendCompletedEvent(any());
    }

    @Test
    void consumeLogsAggregationRequest_validRequest_triggersOrchestratorAndPublishesResult() {
        UUID jobId = UUID.randomUUID();
        LogsAggregationRequest request = LogsAggregationRequest.builder()
                .jobId(jobId)
                .build();

        LogsAggregationCompletedEvent completedEvent = LogsAggregationCompletedEvent.builder()
                .jobId(jobId)
                .status("SUCCESS")
                .build();

        given(orchestratorService.executeLogsAggregationJob(request)).willReturn(completedEvent);

        kafkaConsumer.consumeLogsAggregationRequest(request);

        verify(orchestratorService).executeLogsAggregationJob(request);
        verify(kafkaProducer).sendCompletedEvent(completedEvent);
    }
}
