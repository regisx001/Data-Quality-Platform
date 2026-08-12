package com.regisx001.dQul.logs.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "realtime_log_metrics", indexes = {
    @Index(name = "idx_realtime_log_metrics_window", columnList = "window_start, window_end")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RealtimeLogMetricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "window_start", nullable = false, length = 64)
    private String windowStart;

    @Column(name = "window_end", nullable = false, length = 64)
    private String windowEnd;

    @Column(name = "throughput_logs_per_sec", nullable = false)
    private Double throughputLogsPerSec;

    @Column(name = "total_logs_count", nullable = false)
    private Long totalLogsCount;

    @Column(name = "info_count")
    private Long infoCount;

    @Column(name = "warn_count")
    private Long warnCount;

    @Column(name = "error_count")
    private Long errorCount;

    @Column(name = "debug_count")
    private Long debugCount;

    @Column(name = "avg_execution_time_ms")
    private Double avgExecutionTimeMs;

    @Column(name = "service_breakdown", columnDefinition = "TEXT")
    private String serviceBreakdown;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
