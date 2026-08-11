package com.regisx001.dQul.compute.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetProfileRequest {

    private UUID profileId;
    private UUID datasetId;

    /**
     * Source type: S3_CSV, POSTGRES_JDBC, etc.
     */
    private String sourceType;

    /**
     * S3 / S3A URI for CSV file (e.g. s3a://dqul-bucket/datasets/sales.csv)
     */
    private String s3aUri;

    /**
     * Optional Spark CSV read options (e.g. header, inferSchema, delimiter)
     */
    private Map<String, String> csvOptions;

    /**
     * JDBC configuration for Postgres profiling
     */
    private JdbcConfig jdbcConfig;

    @Builder.Default
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JdbcConfig {
        private String url;
        private String dbtable;
        private String user;
        private String password;
        private String driver;
    }
}
