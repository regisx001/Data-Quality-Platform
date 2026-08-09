package com.regisx001.dQul.security.filters;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Logs every HTTP request to the application's SLF4J log.
 *
 * <p>
 * Captures method, path, response status, duration, client IP and user agent.
 * Runs for all requests (registered as a {@link Component} servlet filter, before
 * the dispatcher), so it is independent of Spring Security filter ordering.
 *
 * <p>
 * Never logs sensitive data: no Authorization header, no JWT, no request body,
 * no query-string secrets. The user agent is newline-sanitized.
 */
@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

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
        String method = request.getMethod();
        String path = request.getRequestURI();
        String query = request.getQueryString();
        if (query != null && !query.isBlank()) {
            path = path + "?" + query;
        }
        int status = response.getStatus();
        log.info("HTTP {} {} -> {} ({} ms) ip={} userAgent={} success={}",
                method, path, status, duration, clientIp(request), sanitize(request.getHeader("User-Agent")),
                success);
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
