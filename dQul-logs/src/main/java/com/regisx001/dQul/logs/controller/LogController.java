package com.regisx001.dQul.logs.controller;

import com.regisx001.dQul.logs.common.error.ResourceNotFoundException;
import com.regisx001.dQul.logs.domain.LogEntry;
import com.regisx001.dQul.logs.dto.LogPageDto;
import com.regisx001.dQul.logs.dto.LogStatsDto;
import com.regisx001.dQul.logs.service.LogService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
@Slf4j
@Validated
@CrossOrigin(origins = "*")
public class LogController {

    private final LogService logService;

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
        Page<LogEntry> logs = logService.queryLogs(search, level, serviceName, category, traceId, pageable);
        return ResponseEntity.ok(LogPageDto.builder()
                .content(logs.getContent())
                .page(logs.getNumber())
                .size(logs.getSize())
                .totalElements(logs.getTotalElements())
                .totalPages(logs.getTotalPages())
                .first(logs.isFirst())
                .last(logs.isLast())
                .hasNext(logs.hasNext())
                .hasPrevious(logs.hasPrevious())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LogEntry> getLogById(@PathVariable UUID id) {
        return ResponseEntity.ok(logService.getLogById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Log entry not found: " + id)));
    }

    @GetMapping("/stats")
    public ResponseEntity<LogStatsDto> getStats() {
        return ResponseEntity.ok(logService.getLogStats());
    }

    @DeleteMapping("/purge")
    public ResponseEntity<Map<String, String>> purgeLogs(
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        logService.purgeLogsOlderThan(days);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Logs older than " + days + " days purged"));
    }
}
