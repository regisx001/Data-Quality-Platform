package com.regisx001.dQul.logs.kafka;

import com.regisx001.dQul.logs.dto.batch.LogsAggregationCompletedEvent;
import com.regisx001.dQul.logs.service.BatchLogMetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaBatchCompletionConsumer {

    private final BatchLogMetricService batchLogMetricService;

    @KafkaListener(
        topics = "${dqul.kafka.topics.logs-aggregate-result:dqul.logs.aggregate.result}",
        groupId = "${spring.kafka.consumer.group-id:dqul-logs-batch-group}",
        containerFactory = "batchCompletionKafkaListenerContainerFactory"
    )
    public void consumeBatchCompletionEvent(LogsAggregationCompletedEvent event) {
        if (event == null) {
            log.warn("Received null Spark batch completion event; skipping");
            return;
        }

        try {
            log.info("Received Spark batch completion event for jobId={} with status={}", event.getJobId(), event.getStatus());
            batchLogMetricService.saveBatchMetric(event);
        } catch (Exception e) {
            log.error("Internal error persisting batch log metric for jobId={}: {}", event.getJobId(), e.getMessage(), e);
            throw e;
        }
    }
}
