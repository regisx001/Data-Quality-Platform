package com.regisx001.dQul.common.service;

import com.regisx001.dQul.common.config.KafkaConfig;
import com.regisx001.dQul.common.dto.LogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Produces platform log events to the {@code platform-logs-topic} Kafka topic
 * so they can be
 * consumed, validated, and persisted by the standalone {@code dQul-logs}
 * microservice.
 *
 * <p>
 * Each record is sent with the message <b>key</b> set to the event's
 * {@code traceId} —
 * the broker uses it for partitioning and per-trace ordering (see the topic
 * contract).
 *
 * <p>
 * This producer is <b>not wired into the application's runtime behaviour</b>:
 * it exposes a
 * self-contained {@code send}/{@code produce} API that callers may use when
 * they want to emit
 * log events. No existing service/controller references it, so enabling it is
 * opt-in.
 *
 * <p>
 * Requires the Kafka producer config in {@code application.yaml}
 * ({@code spring.kafka.producer.*}: {@code JsonSerializer} value, key =
 * String), and the
 * {@code KafkaTemplate} auto-configured by Spring Boot whenever
 * {@code spring-kafka} is on the
 * classpath.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LogsProducer {

    private final KafkaTemplate<String, LogEvent> kafkaTemplate;

    /**
     * Builds a sane default event, fills any fields left {@code null} with
     * publish-time defaults,
     * and sends it to {@code platform-logs-topic} under the {@code traceId} key.
     * The send is
     * asynchronous; completion is observed via a callback that logs
     * success/failure.
     *
     * @return the {@code traceId} used as the message key
     */
    public String produce(LogEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("LogEvent must not be null");
        }
        LogEvent resolved = resolveDefaults(event);
        send(resolved);
        return resolved.getTraceId();
    }

    /**
     * Asynchronously sends an event (already resolved) to
     * {@code platform-logs-topic}.
     * Completion is observed via a callback that logs success/failure.
     */
    public void send(LogEvent event) {
        String key = event.getTraceId();
        CompletableFuture<SendResult<String, LogEvent>> future = kafkaTemplate.send(KafkaConfig.LOGS_TOPIC, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send log event to {} (key={}): {}",
                        KafkaConfig.LOGS_TOPIC, key, ex.getMessage(), ex);
            } else if (result != null) {
                log.debug("Sent log event to topic={} partition={} offset={} key={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        key);
            }
        });
    }

    /**
     * Applies contract defaults for missing fields and stamps a fresh
     * {@code traceId}/
     * {@code timestamp} when not provided, so callers can publish a minimal event.
     */
    private LogEvent resolveDefaults(LogEvent event) {
        LogEvent.LogEventBuilder b = event.toBuilder();
        b.traceId(event.getTraceId() != null ? event.getTraceId() : UUID.randomUUID().toString());
        b.serviceName(event.getServiceName() != null && !event.getServiceName().isBlank()
                ? event.getServiceName().trim()
                : "unknown-service");
        b.logLevel(event.getLogLevel() != null && !event.getLogLevel().isBlank()
                ? event.getLogLevel().trim().toUpperCase()
                : "INFO");
        b.category(event.getCategory() != null && !event.getCategory().isBlank()
                ? event.getCategory().trim().toUpperCase()
                : "INTERNAL_LOG");
        b.timestamp(event.getTimestamp() != null ? event.getTimestamp() : Instant.now());
        return b.build();
    }
}
