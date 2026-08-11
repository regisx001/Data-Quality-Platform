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
    private String sourceType;
    private String s3aUri;
    private Map<String, String> csvOptions;
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
