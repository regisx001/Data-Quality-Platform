package com.regisx001.dQul.logs.domain;

import com.regisx001.dQul.logs.dto.batch.LogsAggregationResultDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "batch_log_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchLogMetricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "from_timestamp", length = 64)
    private String fromTimestamp;

    @Column(name = "to_timestamp", length = 64)
    private String toTimestamp;

    @Column(name = "total_logs_count")
    private Long totalLogsCount;

    @Column(name = "avg_execution_time_ms")
    private Double avgExecutionTimeMs;

    @Column(name = "minio_storage_path", length = 512)
    private String minioStoragePath;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_data", columnDefinition = "jsonb")
    private LogsAggregationResultDto resultData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
