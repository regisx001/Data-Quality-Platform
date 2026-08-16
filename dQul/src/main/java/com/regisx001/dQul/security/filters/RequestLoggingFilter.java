package com.regisx001.dQul.security.filters;

import java.io.IOException;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.regisx001.dQul.common.dto.LogEvent;
import com.regisx001.dQul.common.service.LogsProducer;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Logs HTTP requests to SLF4J and publishes structured telemetry events to Kafka.
 *
 * <p>
 * Captures HTTP method, path, response status, execution duration, and client IP.
 * Emits HTTP telemetry events to {@code platform-logs-topic} for consumption by {@code dQul-logs}.
 */
@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final LogsProducer logsProducer;

    public RequestLoggingFilter(@Autowired(required = false) LogsProducer logsProducer) {
        this.logsProducer = logsProducer;
    }

    public RequestLoggingFilter() {
        this.logsProducer = null;
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
        if (isIgnoredAsset(rawPath)) {
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

        emitHttpTelemetryEvent(method, rawPath, status, duration);
    }

    private void emitHttpTelemetryEvent(String method, String path, int status, long duration) {
        if (logsProducer == null) {
            return;
        }
        try {
            String logLevel = status >= 500 ? "ERROR" : (status >= 400 ? "WARN" : "INFO");
            String category = path.contains("/auth") ? "AUTH" : (path.contains("/datasets") ? "INGESTION" : "API");
            String message = String.format("HTTP %s %s -> %d (%d ms)", method, path, status, duration);

            LogEvent logEvent = LogEvent.builder()
                    .serviceName("dQul-api")
                    .logLevel(logLevel)
                    .category(category)
                    .message(message)
                    .path(path)
                    .httpMethod(method)
                    .statusCode(status)
                    .executionTimeMs(duration)
                    .timestamp(Instant.now())
                    .build();

            logsProducer.send(logEvent);
        } catch (Exception e) {
            log.debug("Could not publish HTTP telemetry event to Kafka: {}", e.getMessage());
        }
    }

    private boolean isIgnoredAsset(String path) {
        if (path == null) return true;
        return path.equals("/health")
                || path.startsWith("/actuator")
                || path.contains("/swagger")
                || path.contains("/v3/api-docs")
                || path.endsWith(".ico")
                || path.endsWith(".js")
                || path.endsWith(".css");
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
