package com.regisx001.dQul.logs.kafka;

import com.regisx001.dQul.compute.dto.logs.LogsAggregationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogsAggregationKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${dqul.kafka.topics.logs-aggregate-request:dqul.logs.aggregate.request}")
    private String logsAggregateRequestTopic;

    public void sendAggregationRequest(LogsAggregationRequest request) {
        String key = request.getJobId() != null ? request.getJobId().toString() : "global";
        log.info("Publishing batch logs aggregation request for jobId={} to topic={}", key, logsAggregateRequestTopic);

        kafkaTemplate.send(logsAggregateRequestTopic, key, request)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish logs aggregation request for jobId={}: {}", key, ex.getMessage(), ex);
                    } else {
                        log.info("Successfully sent logs aggregation request for jobId={} to topic={} at offset={}",
                                key, logsAggregateRequestTopic, result.getRecordMetadata().offset());
                    }
                });
    }
}
