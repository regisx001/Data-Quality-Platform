package com.regisx001.dQul.logs.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * HTTP analytics derived from {@code path}, {@code httpMethod} and
 * {@code statusCode}: status-code distribution, method breakdown, and per
 * endpoint rollups.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HttpAnalyticsDto {

    private long totalRequests;
    private long count2xx;
    private long count3xx;
    private long count4xx;
    private long count5xx;
    private double rate2xx;
    private double rate3xx;
    private double rate4xx;
    private double rate5xx;

    /** Status code -> count (e.g. 200 -> 1200). */
    private Map<Integer, Long> statusCounts;

    /** HTTP method -> count. */
    private Map<String, Long> methodCounts;

    /** Endpoint rollups, highest volume first. */
    private List<EndpointAnalyticsDto> endpoints;
}
