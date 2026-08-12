package com.regisx001.dQul.compute.kafka;

import com.regisx001.dQul.compute.dto.DatasetProfileCompletedEvent;
import com.regisx001.dQul.compute.dto.DatasetProfileRequest;
import com.regisx001.dQul.compute.service.ProfileJobOrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProfileKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProfileKafkaConsumer.class);

    private final ProfileJobOrchestratorService orchestratorService;
    private final ProfileKafkaProducer kafkaProducer;

    public ProfileKafkaConsumer(ProfileJobOrchestratorService orchestratorService,
                                ProfileKafkaProducer kafkaProducer) {
        this.orchestratorService = orchestratorService;
        this.kafkaProducer = kafkaProducer;
    }

    @KafkaListener(
            topics = "${dqul.kafka.topics.profile-request:dqul.dataset.profile.request}",
            groupId = "${spring.kafka.consumer.group-id:dqul-compute-group}"
    )
    public void consumeProfileRequest(DatasetProfileRequest request) {
        if (request == null) {
            log.warn("Received null DatasetProfileRequest message; ignoring.");
            return;
        }

        log.info("Received profiling request event for profileId={}, datasetId={}", request.getProfileId(), request.getDatasetId());

        DatasetProfileCompletedEvent completedEvent = orchestratorService.executeProfilingJob(request);
        kafkaProducer.sendCompletedEvent(completedEvent);
    }
}
