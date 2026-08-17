package com.regisx001.dQul.logs.config.filter;

import com.regisx001.dQul.logs.config.KafkaConfig;
import com.regisx001.dQul.logs.dto.LogIngestionDto;
import com.regisx001.dQul.logs.service.LogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Filter that logs incoming HTTP requests for {@code dQul-logs} and publishes HTTP request telemetry to Kafka.
 */
@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final LogService logService;
    private final List<String> excludePaths;

    @Autowired
    public RequestLoggingFilter(
            @Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate,
            @Autowired(required = false) LogService logService,
            @Value("${dqul.logging.http.exclude-paths:/health,/actuator,/swagger,/v3/api-docs,/api/v1/logs/stream,.ico,.js,.css}") List<String> excludePaths) {
        this.kafkaTemplate = kafkaTemplate;
        this.logService = logService;
        this.excludePaths = excludePaths != null ? excludePaths : Collections.emptyList();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        long start = System.currentTimeMillis();
        boolean success = true;
        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            success = false;
            throw e;
        } finally {
            logRequest(request, response, System.currentTimeMillis() - start, success);
        }
    }

    private void logRequest(HttpServletRequest request, HttpServletResponse response, long duration,
            boolean success) {
        String rawPath = request.getRequestURI();
        if (isExcluded(rawPath)) {
            return;
        }

        String method = request.getMethod();
        int status = response.getStatus();
        String path = rawPath;
        String query = request.getQueryString();
        if (query != null && !query.isBlank()) {
            path = path + "?" + query;
        }

        log.info("HTTP {} {} -> {} ({} ms) ip={} userAgent={} success={}",
                method, path, status, duration, clientIp(request), sanitize(request.getHeader("User-Agent")),
                success);

        recordHttpTelemetryEvent(method, rawPath, status, duration);
    }

    private void recordHttpTelemetryEvent(String method, String path, int status, long duration) {
        try {
            String logLevel = status >= 500 ? "ERROR" : (status >= 400 ? "WARN" : "INFO");
            String message = String.format("HTTP %s %s -> %d (%d ms)", method, path, status, duration);

            LogIngestionDto dto = LogIngestionDto.builder()
                    .traceId(UUID.randomUUID().toString())
                    .serviceName("dQul-logs")
                    .logLevel(logLevel)
                    .category("INTERNAL_LOG")
                    .message(message)
                    .path(path)
                    .httpMethod(method)
                    .statusCode(status)
                    .executionTimeMs(duration)
                    .timestamp(Instant.now())
                    .build();

            if (kafkaTemplate != null) {
                kafkaTemplate.send(KafkaConfig.LOGS_TOPIC, dto.getTraceId(), dto);
            } else if (logService != null) {
                logService.saveLog(dto);
            }
        } catch (Exception e) {
            log.error("Failed to record HTTP telemetry for dQul-logs: {}", e.getMessage(), e);
        }
    }

    private boolean isExcluded(String path) {
        if (path == null || path.isBlank()) {
            return true;
        }
        for (String pattern : excludePaths) {
            if (pattern != null && !pattern.isBlank()) {
                String trimmed = pattern.trim();
                if (path.equals(trimmed) || path.startsWith(trimmed) || path.contains(trimmed) || path.endsWith(trimmed)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String sanitize(String value) {
        return value == null ? "-" : value.replaceAll("[\\r\\n]", " ");
    }
}
