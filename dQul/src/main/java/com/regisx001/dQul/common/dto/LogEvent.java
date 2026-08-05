package com.regisx001.dQul.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Log event payload produced to the {@code platform-logs-topic} Kafka topic.
 *
 * <p>
 * Field names/types match the message contract consumed by the standalone
 * {@code dQul-logs} microservice (its {@code LogIngestionDto}). The record key
 * is the
 * {@code traceId}, which the broker uses for partitioning and per-trace
 * ordering.
 *
 * @see <a href="../../../dQul-logs/docs/topic-contract.md">platform-logs-topic
 *      contract</a>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class LogEvent {

    /** Used as the Kafka record key. Max 64 chars. */
    private String traceId;

    private String serviceName;

    /** One of TRACE|DEBUG|INFO|WARN|ERROR|FATAL (case-insensitive). */
    private String logLevel;

    /** Free-form, uppercased, max 32 chars. */
    private String category;

    /** Required — must not be blank. */
    private String message;

    private String stackTrace;

    private String path;

    private String httpMethod;

    private Integer statusCode;

    private Long executionTimeMs;

    private String userId;

    private String userEmail;

    /** Free-form JSON as a string. */
    private String metadata;

    private Instant timestamp;
}
