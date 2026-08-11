package com.regisx001.dQul.compute.kafka;

import com.regisx001.dQul.compute.dto.DatasetProfileCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProfileKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(ProfileKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String resultTopic;

    public ProfileKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate,
                               @Value("${dqul.kafka.topics.profile-result:dqul.dataset.profile.result}") String resultTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.resultTopic = resultTopic;
    }

    public void sendCompletedEvent(DatasetProfileCompletedEvent event) {
        String key = event.getProfileId() != null ? event.getProfileId().toString() : event.getDatasetId().toString();
        log.info("Publishing DatasetProfileCompletedEvent to topic '{}' with key '{}', status='{}'",
                resultTopic, key, event.getStatus());

        kafkaTemplate.send(resultTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish DatasetProfileCompletedEvent for key '{}': {}", key, ex.getMessage(), ex);
                    } else {
                        log.info("Successfully published DatasetProfileCompletedEvent for key '{}' to partition {} at offset {}",
                                key, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
    }
}
