package com.regisx001.dQul.logs.service;

import com.regisx001.dQul.logs.dto.streaming.RealtimeLogMetricsDto;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Service managing real-time Server-Sent Events (SSE) connections,
 * Redis Pub/Sub listening, and timeseries historical window persistence.
 */
public interface RealtimeLogSseService {

    /**
     * Registers a new client SseEmitter connection.
     *
     * @return SseEmitter instance
     */
    SseEmitter subscribeSse();

    /**
     * Processes incoming real-time metric payload: saves to DB & broadcasts to active SSE subscribers.
     *
     * @param metricDto the window metric payload
     */
    void processAndBroadcastMetric(RealtimeLogMetricsDto metricDto);

    /**
     * Retrieves recent historical window metrics for trend visualization.
     *
     * @param limit number of recent window metrics to retrieve
     * @return list of historical RealtimeLogMetricsDto snapshots
     */
    List<RealtimeLogMetricsDto> getHistoricalMetrics(int limit);
}
