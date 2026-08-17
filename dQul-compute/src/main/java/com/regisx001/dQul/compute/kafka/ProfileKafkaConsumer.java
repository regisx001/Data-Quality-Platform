package com.regisx001.dQul.compute.kafka;

import com.regisx001.dQul.compute.dto.DatasetProfileCompletedEvent;
import com.regisx001.dQul.compute.dto.DatasetProfileRequest;
import com.regisx001.dQul.compute.service.ProfileJobOrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ProfileKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProfileKafkaConsumer.class);

    private final ProfileJobOrchestratorService orchestratorService;
    private final ProfileKafkaProducer kafkaProducer;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${KAFKA_TOPIC_PLATFORM_LOGS:platform-logs-topic}")
    private String logsTopic;

    public ProfileKafkaConsumer(ProfileJobOrchestratorService orchestratorService,
                                ProfileKafkaProducer kafkaProducer,
                                @Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate) {
        this.orchestratorService = orchestratorService;
        this.kafkaProducer = kafkaProducer;
        this.kafkaTemplate = kafkaTemplate;
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
        long start = System.currentTimeMillis();

        try {
            DatasetProfileCompletedEvent completedEvent = orchestratorService.executeProfilingJob(request);
            kafkaProducer.sendCompletedEvent(completedEvent);
            emitProfilingLog(request, System.currentTimeMillis() - start, true, null);
        } catch (Exception e) {
            log.error("Spark batch profiling failed for datasetId={}: {}", request.getDatasetId(), e.getMessage(), e);
            emitProfilingLog(request, System.currentTimeMillis() - start, false, e.getMessage());
            throw e;
        }
    }

    private void emitProfilingLog(DatasetProfileRequest request, long duration, boolean success, String errorMessage) {
        if (kafkaTemplate == null) return;
        try {
            String traceId = UUID.randomUUID().toString();
            String logLevel = success ? "INFO" : "ERROR";
            String path = String.format("/api/v1/datasets/%s/profile", request.getDatasetId());
            String message = success
                    ? String.format("Spark batch dataset profiling completed for datasetId=%s (profileId=%s) in %d ms",
                            request.getDatasetId(), request.getProfileId(), duration)
                    : String.format("Spark batch dataset profiling failed for datasetId=%s: %s",
                            request.getDatasetId(), errorMessage);

            Map<String, Object> logEvent = new HashMap<>();
            logEvent.put("traceId", traceId);
            logEvent.put("serviceName", "dQul-compute");
            logEvent.put("logLevel", logLevel);
            logEvent.put("category", "PROFILING");
            logEvent.put("message", message);
            logEvent.put("path", path);
            logEvent.put("httpMethod", "POST");
            logEvent.put("statusCode", success ? 200 : 500);
            logEvent.put("executionTimeMs", duration);
            logEvent.put("timestamp", Instant.now().toString());

            kafkaTemplate.send(logsTopic, traceId, logEvent);
        } catch (Exception e) {
            log.debug("Failed to emit dQul-compute profiling log event: {}", e.getMessage());
        }
    }
}
