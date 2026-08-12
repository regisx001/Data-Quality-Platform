package com.regisx001.dQul.compute.kafka;

import com.regisx001.dQul.compute.dto.logs.LogsAggregationCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class LogsKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(LogsKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${dqul.kafka.topics.logs-aggregate-result:dqul.logs.aggregate.result}")
    private String logsAggregateResultTopic;

    public LogsKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendCompletedEvent(LogsAggregationCompletedEvent event) {
        String key = event.getJobId() != null ? event.getJobId().toString() : "global";
        log.info("Publishing logs aggregation completion event for jobId={} to topic={}", key, logsAggregateResultTopic);

        kafkaTemplate.send(logsAggregateResultTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish logs aggregation completion event for jobId={}: {}", key, ex.getMessage(), ex);
                    } else {
                        log.info("Successfully sent logs aggregation completion event for jobId={} to offset={}",
                                key, result.getRecordMetadata().offset());
                    }
                });
    }
}
