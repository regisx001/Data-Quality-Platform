package com.regisx001.dQul.logs.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "log_entries", indexes = {
    @Index(name = "idx_log_trace_id", columnList = "trace_id"),
    @Index(name = "idx_log_service_name", columnList = "service_name"),
    @Index(name = "idx_log_level", columnList = "log_level"),
    @Index(name = "idx_log_timestamp", columnList = "timestamp"),
    @Index(name = "idx_log_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "service_name", nullable = false, length = 64)
    private String serviceName;

    @Column(name = "log_level", nullable = false, length = 16)
    private String logLevel;

    @Column(name = "category", nullable = false, length = 32)
    private String category;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "path", length = 512)
    private String path;

    @Column(name = "http_method", length = 16)
    private String httpMethod;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "user_id", length = 128)
    private String userId;

    @Column(name = "user_email", length = 128)
    private String userEmail;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
}
