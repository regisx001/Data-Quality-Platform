package com.regisx001.dQul.dataset.kafka;

import com.regisx001.dQul.compute.dto.DatasetProfileCompletedEvent;
import com.regisx001.dQul.dataset.service.ProfileResultProcessorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProfileResultKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProfileResultKafkaConsumer.class);

    private final ProfileResultProcessorService processorService;

    public ProfileResultKafkaConsumer(ProfileResultProcessorService processorService) {
        this.processorService = processorService;
    }

    @KafkaListener(
            topics = "${dqul.kafka.topics.profile-result:dqul.dataset.profile.result}",
            groupId = "${spring.kafka.consumer.group-id:dqul-main-group}"
    )
    public void consumeProfileResult(DatasetProfileCompletedEvent event) {
        if (event == null) {
            log.warn("Received null DatasetProfileCompletedEvent message; ignoring.");
            return;
        }

        log.info("Received profiling completion event for profileId={}, datasetId={}, status={}",
                event.getProfileId(), event.getDatasetId(), event.getStatus());

        try {
            processorService.processProfileResult(event);
        } catch (Exception e) {
            log.error("Error processing profiling completion event for profileId={}: {}",
                    event.getProfileId(), e.getMessage(), e);
        }
    }
}
