package com.regisx001.dQul.logs.controller;

import com.regisx001.dQul.logs.config.KafkaConfig;
import com.regisx001.dQul.logs.domain.LogEntry;
import com.regisx001.dQul.logs.dto.LogIngestionDto;
import com.regisx001.dQul.logs.dto.LogStatsDto;
import com.regisx001.dQul.logs.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class LogController {

    private final LogService logService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @PostMapping("/ingest")
    public ResponseEntity<Map<String, String>> ingestLog(@RequestBody LogIngestionDto dto) {
        if (dto.getTimestamp() == null) {
            dto.setTimestamp(Instant.now());
        }
        if (dto.getServiceName() == null) {
            dto.setServiceName("web-frontend");
        }
        kafkaTemplate.send(KafkaConfig.LOGS_TOPIC, dto.getTraceId() != null ? dto.getTraceId() : UUID.randomUUID().toString(), dto);
        return ResponseEntity.accepted().body(Map.of("status", "ACCEPTED", "message", "Log event buffered into Kafka"));
    }

    @GetMapping
    public ResponseEntity<Page<LogEntry>> queryLogs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String traceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<LogEntry> logs = logService.queryLogs(search, level, serviceName, category, traceId, pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LogEntry> getLogById(@PathVariable UUID id) {
        return logService.getLogById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public ResponseEntity<LogStatsDto> getStats() {
        return ResponseEntity.ok(logService.getLogStats());
    }

    @DeleteMapping("/purge")
    public ResponseEntity<Map<String, String>> purgeLogs(@RequestParam(defaultValue = "30") int days) {
        logService.purgeLogsOlderThan(days);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Logs older than " + days + " days purged"));
    }
}
