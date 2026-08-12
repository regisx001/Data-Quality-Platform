package com.regisx001.dQul.compute.kafka;

import com.regisx001.dQul.compute.dto.logs.LogsAggregationCompletedEvent;
import com.regisx001.dQul.compute.dto.logs.LogsAggregationRequest;
import com.regisx001.dQul.compute.service.LogsJobOrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LogsKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(LogsKafkaConsumer.class);

    private final LogsJobOrchestratorService orchestratorService;
    private final LogsKafkaProducer kafkaProducer;

    public LogsKafkaConsumer(LogsJobOrchestratorService orchestratorService,
                             LogsKafkaProducer kafkaProducer) {
        this.orchestratorService = orchestratorService;
        this.kafkaProducer = kafkaProducer;
    }

    @KafkaListener(
            topics = "${dqul.kafka.topics.logs-aggregate-request:dqul.logs.aggregate.request}",
            groupId = "${spring.kafka.consumer.group-id:dqul-compute-group}"
    )
    public void consumeLogsAggregationRequest(LogsAggregationRequest request) {
        if (request == null) {
            log.warn("Received null LogsAggregationRequest message; ignoring.");
            return;
        }

        log.info("Received logs aggregation request event for jobId={}", request.getJobId());

        LogsAggregationCompletedEvent completedEvent = orchestratorService.executeLogsAggregationJob(request);
        kafkaProducer.sendCompletedEvent(completedEvent);
    }
}
