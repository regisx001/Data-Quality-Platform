package com.regisx001.dQul.dataset.kafka;

import com.regisx001.dQul.compute.dto.DatasetProfileRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProfileRequestKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(ProfileRequestKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String profileRequestTopic;

    public ProfileRequestKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate,
                                      @Value("${dqul.kafka.topics.profile-request:dqul.dataset.profile.request}") String profileRequestTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.profileRequestTopic = profileRequestTopic;
    }

    public void sendProfileRequest(DatasetProfileRequest request) {
        String key = request.getProfileId() != null ? request.getProfileId().toString() : request.getDatasetId().toString();
        log.info("Publishing DatasetProfileRequest to topic '{}' with key '{}', sourceType='{}'",
                profileRequestTopic, key, request.getSourceType());

        kafkaTemplate.send(profileRequestTopic, key, request)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish DatasetProfileRequest for key '{}': {}", key, ex.getMessage(), ex);
                    } else {
                        log.info("Successfully published DatasetProfileRequest for key '{}' to partition {} at offset {}",
                                key, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
    }
}
