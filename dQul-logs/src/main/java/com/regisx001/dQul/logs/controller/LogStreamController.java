package com.regisx001.dQul.logs.controller;

import com.regisx001.dQul.logs.dto.streaming.RealtimeLogMetricsDto;
import com.regisx001.dQul.logs.service.RealtimeLogSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/logs/stream")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class LogStreamController {

    private final RealtimeLogSseService realtimeLogSseService;

    /**
     * Establishes a real-time HTML5 Server-Sent Events (SSE) connection.
     * Streams live 5-second tumbling window log metrics directly to the client.
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRealtimeMetrics() {
        log.info("Client connected to real-time log SSE stream /api/v1/logs/stream");
        return realtimeLogSseService.subscribeSse();
    }

    /**
     * Retrieves recent historical window metrics for chart rendering and trend visualization.
     */
    @GetMapping("/history")
    public ResponseEntity<List<RealtimeLogMetricsDto>> getStreamHistory(
            @RequestParam(defaultValue = "60") int limit) {
        return ResponseEntity.ok(realtimeLogSseService.getHistoricalMetrics(limit));
    }
}
