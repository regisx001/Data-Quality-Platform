package com.regisx001.dQul.logs.service;

import com.regisx001.dQul.logs.common.error.LogValidationException;
import com.regisx001.dQul.logs.domain.LogEntry;
import com.regisx001.dQul.logs.domain.LogLevel;
import com.regisx001.dQul.logs.dto.LogIngestionDto;
import com.regisx001.dQul.logs.dto.LogStatsDto;
import com.regisx001.dQul.logs.repository.LogEntryRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogService {

    private final LogEntryRepository logEntryRepository;

    @Transactional
    public LogEntry saveLog(LogIngestionDto dto) {
        return logEntryRepository.save(normalize(dto));
    }

    /**
     * Validates and normalizes an incoming log event into a {@link LogEntry}.
     * Throws {@link LogValidationException} for events that cannot be persisted
     * (null payload, blank message, invalid log level, oversized category). In the
     * Kafka consumer path this exception propagates to the container error handler,
     * which routes the offending record to the dead-letter topic.
     */
    public LogEntry normalize(LogIngestionDto dto) {
        if (dto == null) {
            throw new LogValidationException("Log event payload is null");
        }
        if (dto.getMessage() == null || dto.getMessage().isBlank()) {
            throw new LogValidationException("message is required and must not be blank");
        }

        LogLevel level = dto.getLogLevel() == null || dto.getLogLevel().isBlank()
                ? LogLevel.INFO
                : LogLevel.fromString(dto.getLogLevel())
                        .orElseThrow(() -> new LogValidationException("Invalid logLevel: " + dto.getLogLevel()));

        String category = dto.getCategory() == null || dto.getCategory().isBlank()
                ? "INTERNAL_LOG"
                : dto.getCategory().trim().toUpperCase();
        if (category.length() > 32) {
            throw new LogValidationException("category must be at most 32 characters");
        }

        return LogEntry.builder()
                .traceId(dto.getTraceId())
                .serviceName(dto.getServiceName() != null && !dto.getServiceName().isBlank()
                        ? dto.getServiceName().trim() : "unknown-service")
                .logLevel(level.name())
                .category(category)
                .message(dto.getMessage())
                .stackTrace(dto.getStackTrace())
                .path(dto.getPath())
                .httpMethod(dto.getHttpMethod())
                .statusCode(dto.getStatusCode())
                .executionTimeMs(dto.getExecutionTimeMs())
                .userId(dto.getUserId())
                .userEmail(dto.getUserEmail())
                .metadata(dto.getMetadata())
                .timestamp(dto.getTimestamp() != null ? dto.getTimestamp() : Instant.now())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<LogEntry> queryLogs(String search, String logLevel, String serviceName, String category, String traceId, Pageable pageable) {
        Specification<LogEntry> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (logLevel != null && !logLevel.trim().isEmpty() && !"ALL".equalsIgnoreCase(logLevel)) {
                predicates.add(cb.equal(cb.upper(root.get("logLevel")), logLevel.trim().toUpperCase()));
            }
            if (serviceName != null && !serviceName.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("serviceName")), serviceName.trim().toLowerCase()));
            }
            if (category != null && !category.trim().isEmpty()) {
                predicates.add(cb.equal(cb.upper(root.get("category")), category.trim().toUpperCase()));
            }
            if (traceId != null && !traceId.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("traceId"), traceId.trim()));
            }
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate searchMsg = cb.like(cb.lower(root.get("message")), pattern);
                Predicate searchPath = cb.like(cb.lower(root.get("path")), pattern);
                predicates.add(cb.or(searchMsg, searchPath));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return logEntryRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<LogEntry> getLogById(UUID id) {
        return logEntryRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public LogStatsDto getLogStats() {
        long total = logEntryRepository.count();
        long errorCount = logEntryRepository.countByLogLevel("ERROR") + logEntryRepository.countByLogLevel("FATAL");
        long warnCount = logEntryRepository.countByLogLevel("WARN");
        long infoCount = logEntryRepository.countByLogLevel("INFO");

        double errorRate = total > 0 ? ((double) errorCount / total) * 100.0 : 0.0;

        Instant past24h = Instant.now().minus(24, ChronoUnit.HOURS);
        Double avgLatency = logEntryRepository.getAverageExecutionTimeSince(past24h);

        Map<String, Long> byService = new HashMap<>();
        for (Object[] row : logEntryRepository.countLogsByService()) {
            byService.put((String) row[0], (Long) row[1]);
        }

        Map<String, Long> byCategory = new HashMap<>();
        for (Object[] row : logEntryRepository.countLogsByCategory()) {
            byCategory.put((String) row[0], (Long) row[1]);
        }

        return LogStatsDto.builder()
                .totalLogs(total)
                .errorCount(errorCount)
                .warnCount(warnCount)
                .infoCount(infoCount)
                .errorRatePercentage(Math.round(errorRate * 100.0) / 100.0)
                .averageLatencyMs(avgLatency != null ? Math.round(avgLatency * 100.0) / 100.0 : 0.0)
                .logsByService(byService)
                .logsByCategory(byCategory)
                .build();
    }

    @Transactional
    public void purgeLogsOlderThan(int days) {
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        logEntryRepository.deleteByTimestampBefore(cutoff);
        log.info("Purged logs older than {} days (cutoff: {})", days, cutoff);
    }
}
