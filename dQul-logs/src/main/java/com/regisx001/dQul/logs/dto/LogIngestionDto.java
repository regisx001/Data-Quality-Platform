package com.regisx001.dQul.logs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogIngestionDto {
    private String traceId;
    private String serviceName;
    private String logLevel;
    private String category;
    private String message;
    private String stackTrace;
    private String path;
    private String httpMethod;
    private Integer statusCode;
    private Long executionTimeMs;
    private String userId;
    private String userEmail;
    private String metadata;
    private Instant timestamp;
}
