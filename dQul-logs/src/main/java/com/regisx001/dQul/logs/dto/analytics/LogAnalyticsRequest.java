package com.regisx001.dQul.logs.dto.analytics;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Immutable analytics query: a time window (inclusive) plus optional
 * dimensional filters. Granularity controls the volume time-series bucketing
 * (PT1M, PT1H, PT24H…). Empty nullable filters mean "no restriction".
 */
@Data
@Builder
public class LogAnalyticsRequest {

    private Instant from;
    private Instant to;

    /** Bucket size for the volume time series, e.g. "PT1H". Defaults to PT1H. */
    private String granularity;

    private String serviceName;
    private String category;
    private String traceId;
}
