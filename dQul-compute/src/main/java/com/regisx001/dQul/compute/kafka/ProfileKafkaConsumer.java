package com.regisx001.dQul.compute.kafka;

import com.regisx001.dQul.compute.dto.DatasetProfileCompletedEvent;
import com.regisx001.dQul.compute.dto.DatasetProfileRequest;
import com.regisx001.dQul.compute.dto.TableProfileDto;
import com.regisx001.dQul.compute.service.DatasetProfilerService;
import com.regisx001.dQul.compute.service.S3ProfileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ProfileKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProfileKafkaConsumer.class);

    private final DatasetProfilerService profilerService;
    private final S3ProfileStorageService s3StorageService;
    private final ProfileKafkaProducer kafkaProducer;

    public ProfileKafkaConsumer(DatasetProfilerService profilerService,
                                S3ProfileStorageService s3StorageService,
                                ProfileKafkaProducer kafkaProducer) {
        this.profilerService = profilerService;
        this.s3StorageService = s3StorageService;
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

        log.info("Received profiling request for profileId={}, datasetId={}", request.getProfileId(), request.getDatasetId());
        long startTime = System.currentTimeMillis();

        try {
            // 1. Compute profile via Spark
            TableProfileDto tableProfile = profilerService.profile(request);

            // 2. Save single JSON document to S3/MinIO
            String s3ResultUri = s3StorageService.saveProfileResult(tableProfile);

            long duration = System.currentTimeMillis() - startTime;

            // 3. Emit completion event
            DatasetProfileCompletedEvent completedEvent = DatasetProfileCompletedEvent.builder()
                    .profileId(request.getProfileId())
                    .datasetId(request.getDatasetId())
                    .status("SUCCESS")
                    .s3ResultUri(s3ResultUri)
                    .rowCount(tableProfile.getRowCount())
                    .columnCount(tableProfile.getColumnCount())
                    .executionDurationMs(duration)
                    .completedAt(LocalDateTime.now())
                    .build();

            kafkaProducer.sendCompletedEvent(completedEvent);
            log.info("Successfully completed dataset profiling for profileId={} in {} ms", request.getProfileId(), duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to complete dataset profiling for profileId={}: {}", request.getProfileId(), e.getMessage(), e);

            DatasetProfileCompletedEvent failedEvent = DatasetProfileCompletedEvent.builder()
                    .profileId(request.getProfileId())
                    .datasetId(request.getDatasetId())
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .executionDurationMs(duration)
                    .completedAt(LocalDateTime.now())
                    .build();

            kafkaProducer.sendCompletedEvent(failedEvent);
        }
    }
}
