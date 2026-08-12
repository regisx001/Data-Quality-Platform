package com.regisx001.dQul.logs.controller;

import com.regisx001.dQul.logs.common.error.ResourceNotFoundException;
import com.regisx001.dQul.logs.domain.LogEntry;
import com.regisx001.dQul.logs.dto.LogPageDto;
import com.regisx001.dQul.logs.dto.LogQueryResultDto;
import com.regisx001.dQul.logs.dto.LogStatsDto;
import com.regisx001.dQul.logs.dto.analytics.LogAnalyticsDto;
import com.regisx001.dQul.logs.dto.analytics.LogAnalyticsRequest;
import com.regisx001.dQul.logs.service.LogAnalyticsService;
import com.regisx001.dQul.logs.service.LogService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.regisx001.dQul.compute.dto.logs.LogsAggregationRequest;
import com.regisx001.dQul.logs.kafka.LogsAggregationKafkaProducer;

import com.regisx001.dQul.logs.dto.batch.BatchLogMetricDto;
import com.regisx001.dQul.logs.service.BatchLogMetricService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
@Slf4j
@Validated
@CrossOrigin(origins = "*")
public class LogController {

    private final LogService logService;
    private final LogAnalyticsService logAnalyticsService;
    private final LogsAggregationKafkaProducer aggregationKafkaProducer;
    private final BatchLogMetricService batchLogMetricService;

    /**
     * Retrieves recent historical Spark batch analytics executions.
     */
    @GetMapping("/batch/history")
    public ResponseEntity<List<BatchLogMetricDto>> getBatchHistory(
            @RequestParam(defaultValue = "30") int limit) {
        return ResponseEntity.ok(batchLogMetricService.getRecentBatchMetrics(limit));
    }

    /**
     * Triggers an asynchronous Spark batch logs aggregation job by publishing
     * a clean request event to Kafka (consumed by dQul-compute microservice).
     */
    @PostMapping("/aggregate")
    public ResponseEntity<Map<String, Object>> triggerBatchAggregation(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        UUID jobId = UUID.randomUUID();

        // Create PENDING record in batch_log_metrics PostgreSQL table immediately
        batchLogMetricService.createPendingBatchMetric(jobId, from, to);

        LogsAggregationRequest event = LogsAggregationRequest.builder()
                .jobId(jobId)
                .from(from)
                .to(to)
                .build();

        log.info("Triggering Spark batch logs aggregation for jobId={}", jobId);
        aggregationKafkaProducer.sendAggregationRequest(event);

        return ResponseEntity.accepted().body(Map.of(
                "jobId", jobId,
                "status", "BATCH_AGGREGATION_REQUESTED",
                "message", "Batch logs aggregation request published to Kafka"
        ));
    }

    @GetMapping
    public ResponseEntity<LogPageDto> queryLogs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String traceId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        LogQueryResultDto result = logService.queryLogs(search, level, serviceName, category, traceId, pageable);
        List<LogEntry> content = result.getContent();
        long totalElements = result.getTotalElements();
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        boolean hasNext = page + 1 < totalPages;
        return ResponseEntity.ok(LogPageDto.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(!hasNext)
                .hasNext(hasNext)
                .hasPrevious(page > 0)
                .build());
    }

    /**
     * @deprecated Legacy database stats endpoint. Use Realtime SSE Stream (/stream)
     *             or Spark Batch Analytics (/batch/history) instead.
     */
    @Deprecated(since = "2.0", forRemoval = false)
    @GetMapping("/stats")
    public ResponseEntity<LogStatsDto> getStats() {
        return ResponseEntity.ok(logService.getLogStats());
    }

    /**
     * Aggregates the log store into a structured analytics model across the
     * universal observability dimensions.
     *
     * @deprecated Legacy in-memory / SQL aggregation endpoint. Spark Structured Streaming
     *             (/stream) and Spark Batch Aggregations (/batch/history, /aggregate) are now
     *             the primary engines.
     */
    @Deprecated(since = "2.0", forRemoval = false)
    @GetMapping("/analytics")
    public ResponseEntity<LogAnalyticsDto> getAnalytics(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String granularity,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String traceId) {
        LogAnalyticsRequest request = LogAnalyticsRequest.builder()
                .from(parseInstant(from, "from"))
                .to(parseInstant(to, "to"))
                .granularity(granularity)
                .serviceName(serviceName)
                .category(category)
                .traceId(traceId)
                .build();
        return ResponseEntity.ok(logAnalyticsService.analyze(request));
    }

    private static Instant parseInstant(String value, String param) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid " + param + " (expected ISO-8601 instant): " + value);
        }
    }

    @DeleteMapping("/purge")
    public ResponseEntity<Map<String, String>> purgeLogs(
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        logService.purgeLogsOlderThan(days);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Logs older than " + days + " days purged"));
    }

    // Keep the variable {id} mapping last so literal segments (stats, analytics,
    // purge) always take priority regardless of Spring's pattern matcher.
    @GetMapping("/{id}")
    public ResponseEntity<LogEntry> getLogById(@PathVariable UUID id) {
        return ResponseEntity.ok(logService.getLogById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Log entry not found: " + id)));
    }
}
