package com.regisx001.dQul.logs.service;

import com.regisx001.dQul.logs.config.CacheConfig;
import com.regisx001.dQul.logs.domain.LogEntry;
import com.regisx001.dQul.logs.dto.analytics.*;
import com.regisx001.dQul.logs.repository.LogEntryRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * High-performance {@link LogAnalyticsService} implementation that pushes
 * aggregations down to PostgreSQL via SQL queries and caches results using Redis.
 *
 * @deprecated Legacy SQL aggregation implementation. Replaced by Spark Structured
 *             Streaming (RealtimeLogSseService) and Spark Batch Aggregations (BatchLogMetricService).
 */
@Deprecated(since = "2.0", forRemoval = false)
@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class SqlAggregatedLogAnalyticsService implements LogAnalyticsService {

    private static final int MAX_ERROR_SIGNATURE_ROWS = 10_000;
    private final LogEntryRepository logEntryRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.CACHE_LOG_ANALYTICS, key = "#root.target.analyticsCacheKey(#request)")
    public LogAnalyticsDto analyze(LogAnalyticsRequest request) {
        Instant from = request.getFrom();
        Instant to = request.getTo();
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }

        Instant fromNorm = normalizeFrom(from);
        Instant toNorm = normalizeTo(to);
        String serviceName = cleanFilter(request.getServiceName());
        String category = cleanFilter(request.getCategory());
        String traceId = cleanFilter(request.getTraceId());

        Duration bucket = resolveGranularity(request.getGranularity());

        // 1. Level Analytics (SQL GROUP BY logLevel)
        LevelAnalyticsDto levelsDto = buildLevelsAggregated(fromNorm, toNorm, serviceName, category, traceId);

        long totalLogs = levelsDto.getDistribution().stream()
                .mapToLong(LevelAnalyticsDto.LevelDistribution::getCount)
                .sum();

        // 2. Volume Analytics
        VolumeAnalyticsDto volumeDto = buildVolumeAggregated(fromNorm, toNorm, serviceName, category, traceId, bucket, totalLogs);

        // 3. Service Analytics (SQL GROUP BY serviceName)
        List<ServiceAnalyticsDto> servicesList = buildServicesAggregated(fromNorm, toNorm, serviceName, category, traceId, totalLogs);

        // 4. Category Analytics (SQL GROUP BY category)
        List<CategoryAnalyticsDto> categoriesList = buildCategoriesAggregated(fromNorm, toNorm, serviceName, category, traceId, totalLogs);

        // 5. HTTP Analytics (SQL GROUP BY status_code, http_method, path)
        HttpAnalyticsDto httpDto = buildHttpAggregated(fromNorm, toNorm, serviceName, category, traceId, totalLogs);

        // 6. Latency Analytics (SQL select latencies + Math calculation)
        LatencyAnalyticsDto latencyDto = buildLatencyAggregated(fromNorm, toNorm, serviceName, category, traceId, totalLogs);

        // 7. Error Signatures (bounded sample for pattern normalization)
        List<ErrorSignatureDto> errorSignaturesList = buildErrorSignaturesBounded(fromNorm, toNorm, serviceName, category, traceId);

        // 8. Trace Analytics (SQL/bounded trace aggregation)
        TraceAnalyticsDto tracesDto = buildTracesBounded(fromNorm, toNorm, serviceName, category, traceId);

        // 9. User Analytics (SQL GROUP BY userId)
        Map<String, UserAnalyticsDto> usersMap = buildUsersAggregated(fromNorm, toNorm, serviceName, category, traceId);

        return LogAnalyticsDto.builder()
                .from(from)
                .to(to)
                .totalLogs(totalLogs)
                .volume(volumeDto)
                .levels(levelsDto)
                .services(servicesList)
                .categories(categoriesList)
                .http(httpDto)
                .latency(latencyDto)
                .errorSignatures(errorSignaturesList)
                .traces(tracesDto)
                .users(usersMap)
                .build();
    }

    /**
     * Builds a stable cache key tuple for Spring Cache / Redis.
     */
    public String analyticsCacheKey(LogAnalyticsRequest request) {
        return String.join("|",
                request.getFrom() != null ? request.getFrom().toString() : "DEFAULT_24H",
                request.getTo() != null ? request.getTo().toString() : "NOW",
                nullToEmpty(request.getGranularity()),
                nullToEmpty(request.getServiceName()),
                nullToEmpty(request.getCategory()),
                nullToEmpty(request.getTraceId()));
    }

    // ------------------------------------------------------------------
    // Aggregation Helpers
    // ------------------------------------------------------------------

    private LevelAnalyticsDto buildLevelsAggregated(Instant fromNorm, Instant toNorm,
            String serviceName, String category, String traceId) {
        List<Object[]> rows = logEntryRepository.countLogsByLevelAggregated(fromNorm, toNorm, serviceName, category, traceId);
        Map<String, Long> byLevel = new LinkedHashMap<>();
        List<String> order = List.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL");
        order.forEach(l -> byLevel.put(l, 0L));

        for (Object[] r : rows) {
            String lvl = normLevel((String) r[0]);
            Long cnt = (Long) r[1];
            byLevel.put(lvl, cnt != null ? cnt : 0L);
        }

        long total = Math.max(1L, byLevel.values().stream().mapToLong(Long::longValue).sum());
        long info = byLevel.get("INFO");
        long debug = byLevel.get("DEBUG");
        long trace = byLevel.get("TRACE");
        long warn = byLevel.get("WARN");
        long error = byLevel.get("ERROR");
        long fatal = byLevel.get("FATAL");
        long errorFatal = error + fatal;
        long warnPlus = warn + errorFatal;

        List<LevelAnalyticsDto.LevelDistribution> dist = order.stream()
                .map(l -> LevelAnalyticsDto.LevelDistribution.builder()
                        .level(l)
                        .count(byLevel.get(l))
                        .percentage(round(byLevel.get(l) * 100.0 / total, 2))
                        .build())
                .collect(Collectors.toList());

        return LevelAnalyticsDto.builder()
                .infoCount(info)
                .debugCount(debug)
                .traceCount(trace)
                .warnCount(warn)
                .errorCount(error)
                .fatalCount(fatal)
                .infoRatio(round(info / (double) total, 4))
                .debugRatio(round(debug / (double) total, 4))
                .traceRatio(round(trace / (double) total, 4))
                .warnRatio(round(warn / (double) total, 4))
                .errorRatio(round(error / (double) total, 4))
                .fatalRatio(round(fatal / (double) total, 4))
                .errorRatePercentage(round(errorFatal * 100.0 / total, 2))
                .warnPlusRatePercentage(round(warnPlus * 100.0 / total, 2))
                .distribution(dist)
                .build();
    }

    private VolumeAnalyticsDto buildVolumeAggregated(Instant fromNorm, Instant toNorm, String serviceName,
            String category, String traceId, Duration bucket, long totalLogs) {
        Instant start = alignStart(fromNorm, bucket);
        Instant end = alignEnd(toNorm, bucket);

        TreeMap<Instant, Long> buckets = new TreeMap<>();
        for (Instant t = start; !t.isAfter(end); t = t.plus(bucket)) {
            buckets.put(t, 0L);
        }

        // Aggregate volume buckets via SQL timestamp query or light scan
        Specification<LogEntry> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.between(root.get("timestamp"), fromNorm, toNorm));
            if (serviceName != null) predicates.add(cb.equal(cb.lower(root.get("serviceName")), serviceName.toLowerCase()));
            if (category != null) predicates.add(cb.equal(cb.upper(root.get("category")), category.toUpperCase()));
            if (traceId != null) predicates.add(cb.equal(root.get("traceId"), traceId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // Query timestamp timestamps only
        List<LogEntry> logs = logEntryRepository.findAll(spec);
        for (LogEntry e : logs) {
            Instant key = alignStart(e.getTimestamp(), bucket);
            buckets.compute(key, (k, v) -> v == null ? 1L : v + 1L);
        }

        long seconds = Math.max(1, Duration.between(start, end).getSeconds());
        List<VolumeAnalyticsDto.VolumeBucket> series = buckets.entrySet().stream()
                .map(en -> VolumeAnalyticsDto.VolumeBucket.builder()
                        .bucket(en.getKey().toString())
                        .count(en.getValue())
                        .build())
                .collect(Collectors.toList());

        long max = buckets.values().stream().mapToLong(v -> v).max().orElse(0L);
        long min = buckets.values().stream().mapToLong(v -> v).min().orElse(0L);

        return VolumeAnalyticsDto.builder()
                .logsPerSecond(round(totalLogs / (double) seconds, 3))
                .logsPerMinute(round(totalLogs / (double) seconds * 60.0, 3))
                .maxLogsInBucket(max)
                .minLogsInBucket(min)
                .timeSeries(series)
                .build();
    }

    private List<ServiceAnalyticsDto> buildServicesAggregated(Instant fromNorm, Instant toNorm, String serviceName,
            String category, String traceId, long totalLogs) {
        List<Object[]> rows = logEntryRepository.aggregateServices(fromNorm, toNorm, serviceName, category, traceId);
        List<ServiceAnalyticsDto> result = new ArrayList<>();

        for (Object[] r : rows) {
            String svcName = r[0] == null ? "unknown" : (String) r[0];
            long total = ((Number) r[1]).longValue();
            long errorCount = r[2] != null ? ((Number) r[2]).longValue() : 0L;
            long fatalCount = r[3] != null ? ((Number) r[3]).longValue() : 0L;
            long warnCount = r[4] != null ? ((Number) r[4]).longValue() : 0L;
            double avgLatency = r[5] != null ? ((Number) r[5]).doubleValue() : 0.0;
            double maxLatency = r[6] != null ? ((Number) r[6]).doubleValue() : 0.0;

            List<Long> lats = logEntryRepository.findLatencies(fromNorm, toNorm, serviceName, category, traceId, svcName, null, null, null);

            result.add(ServiceAnalyticsDto.builder()
                    .serviceName(svcName)
                    .totalLogs(total)
                    .logSharePercentage(round(total * 100.0 / Math.max(1, totalLogs), 2))
                    .errorCount(errorCount)
                    .fatalCount(fatalCount)
                    .warnCount(warnCount)
                    .errorRatePercentage(round(errorCount * 100.0 / Math.max(1, total), 2))
                    .averageLatencyMs(round(avgLatency, 2))
                    .p95LatencyMs(round(percentile(lats, 95), 2))
                    .p99LatencyMs(round(percentile(lats, 99), 2))
                    .maxLatencyMs(round(maxLatency, 2))
                    .build());
        }

        result.sort(Comparator.comparingLong(ServiceAnalyticsDto::getTotalLogs).reversed());
        return result;
    }

    private List<CategoryAnalyticsDto> buildCategoriesAggregated(Instant fromNorm, Instant toNorm, String serviceName,
            String category, String traceId, long totalLogs) {
        List<Object[]> rows = logEntryRepository.aggregateCategories(fromNorm, toNorm, serviceName, category, traceId);
        List<CategoryAnalyticsDto> result = new ArrayList<>();

        for (Object[] r : rows) {
            String catName = r[0] == null ? "UNKNOWN" : (String) r[0];
            long total = ((Number) r[1]).longValue();
            long errorCount = r[2] != null ? ((Number) r[2]).longValue() : 0L;
            long fatalCount = r[3] != null ? ((Number) r[3]).longValue() : 0L;
            double avgLatency = r[4] != null ? ((Number) r[4]).doubleValue() : 0.0;

            List<Long> lats = logEntryRepository.findLatencies(fromNorm, toNorm, serviceName, category, traceId, null, catName, null, null);

            result.add(CategoryAnalyticsDto.builder()
                    .category(catName)
                    .totalLogs(total)
                    .logSharePercentage(round(total * 100.0 / Math.max(1, totalLogs), 2))
                    .errorCount(errorCount)
                    .fatalCount(fatalCount)
                    .errorRatePercentage(round(errorCount * 100.0 / Math.max(1, total), 2))
                    .averageLatencyMs(round(avgLatency, 2))
                    .p95LatencyMs(round(percentile(lats, 95), 2))
                    .p99LatencyMs(round(percentile(lats, 99), 2))
                    .build());
        }

        result.sort(Comparator.comparingLong(CategoryAnalyticsDto::getTotalLogs).reversed());
        return result;
    }

    private HttpAnalyticsDto buildHttpAggregated(Instant fromNorm, Instant toNorm, String serviceName,
            String category, String traceId, long totalLogs) {
        Map<Integer, Long> statusCounts = new TreeMap<>();
        List<Object[]> statusRows = logEntryRepository.aggregateHttpStatusCodes(fromNorm, toNorm, serviceName, category, traceId);
        for (Object[] r : statusRows) {
            if (r[0] != null) {
                statusCounts.put(((Number) r[0]).intValue(), ((Number) r[1]).longValue());
            }
        }

        Map<String, Long> methodCounts = new TreeMap<>();
        List<Object[]> methodRows = logEntryRepository.aggregateHttpMethodCounts(fromNorm, toNorm, serviceName, category, traceId);
        for (Object[] r : methodRows) {
            if (r[0] != null) {
                methodCounts.put((String) r[0], ((Number) r[1]).longValue());
            }
        }

        List<Object[]> epRows = logEntryRepository.aggregateHttpEndpoints(fromNorm, toNorm, serviceName, category, traceId);
        List<EndpointAnalyticsDto> endpoints = new ArrayList<>();

        for (Object[] r : epRows) {
            String method = (String) r[0];
            String path = (String) r[1];
            long reqCount = ((Number) r[2]).longValue();
            long errCount = r[3] != null ? ((Number) r[3]).longValue() : 0L;
            double avgLat = r[4] != null ? ((Number) r[4]).doubleValue() : 0.0;
            double maxLat = r[5] != null ? ((Number) r[5]).doubleValue() : 0.0;

            List<Long> lats = logEntryRepository.findLatencies(fromNorm, toNorm, serviceName, category, traceId, null, null, method, path);

            endpoints.add(EndpointAnalyticsDto.builder()
                    .httpMethod(method)
                    .path(path)
                    .requestCount(reqCount)
                    .errorCount(errCount)
                    .errorRatePercentage(round(errCount * 100.0 / Math.max(1, reqCount), 2))
                    .averageLatencyMs(round(avgLat, 2))
                    .p95LatencyMs(round(percentile(lats, 95), 2))
                    .p99LatencyMs(round(percentile(lats, 99), 2))
                    .maxLatencyMs(round(maxLat, 2))
                    .build());
        }

        endpoints.sort(Comparator.comparingLong(EndpointAnalyticsDto::getRequestCount).reversed());

        long c2 = countStatusRange(statusCounts, 200, 299);
        long c3 = countStatusRange(statusCounts, 300, 399);
        long c4 = countStatusRange(statusCounts, 400, 499);
        long c5 = countStatusRange(statusCounts, 500, 599);

        long totalHttp = c2 + c3 + c4 + c5;
        if (totalHttp == 0 && !methodCounts.isEmpty()) {
            totalHttp = methodCounts.values().stream().mapToLong(Long::longValue).sum();
        }
        if (totalHttp == 0 && !endpoints.isEmpty()) {
            totalHttp = endpoints.stream().mapToLong(EndpointAnalyticsDto::getRequestCount).sum();
        }

        long denom = Math.max(1L, totalHttp);

        return HttpAnalyticsDto.builder()
                .totalRequests(totalHttp)
                .count2xx(c2)
                .count3xx(c3)
                .count4xx(c4)
                .count5xx(c5)
                .rate2xx(totalHttp > 0 ? round(c2 * 100.0 / denom, 2) : 0.0)
                .rate3xx(totalHttp > 0 ? round(c3 * 100.0 / denom, 2) : 0.0)
                .rate4xx(totalHttp > 0 ? round(c4 * 100.0 / denom, 2) : 0.0)
                .rate5xx(totalHttp > 0 ? round(c5 * 100.0 / denom, 2) : 0.0)
                .statusCounts(statusCounts)
                .methodCounts(methodCounts)
                .endpoints(endpoints)
                .build();
    }

    private LatencyAnalyticsDto buildLatencyAggregated(Instant fromNorm, Instant toNorm, String serviceName,
            String category, String traceId, long totalLogs) {
        List<Long> lats = logEntryRepository.findLatencies(fromNorm, toNorm, serviceName, category, traceId, null, null, null, null);
        double avg = avg(lats);
        double sd = stddev(lats, avg);
        long slow = lats.stream().filter(l -> l > 1000).count();
        long total = Math.max(1L, totalLogs);

        return LatencyAnalyticsDto.builder()
                .sampleCount(lats.size())
                .averageMs(round(avg, 2))
                .medianMs(round(percentile(lats, 50), 2))
                .minMs(round(min(lats), 2))
                .maxMs(round(max(lats), 2))
                .p50Ms(round(percentile(lats, 50), 2))
                .p75Ms(round(percentile(lats, 75), 2))
                .p90Ms(round(percentile(lats, 90), 2))
                .p95Ms(round(percentile(lats, 95), 2))
                .p99Ms(round(percentile(lats, 99), 2))
                .p999Ms(round(percentile(lats, 99.9), 2))
                .standardDeviationMs(round(sd, 2))
                .varianceMs2(round(sd * sd, 2))
                .slowRequestCount(slow)
                .slowRequestPercentage(round(slow * 100.0 / total, 2))
                .build();
    }

    private List<ErrorSignatureDto> buildErrorSignaturesBounded(Instant fromNorm, Instant toNorm, String serviceName,
            String category, String traceId) {
        Specification<LogEntry> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.between(root.get("timestamp"), fromNorm, toNorm));
            predicates.add(root.get("logLevel").in("ERROR", "FATAL", "error", "fatal"));
            if (serviceName != null) predicates.add(cb.equal(cb.lower(root.get("serviceName")), serviceName.toLowerCase()));
            if (category != null) predicates.add(cb.equal(cb.upper(root.get("category")), category.toUpperCase()));
            if (traceId != null) predicates.add(cb.equal(root.get("traceId"), traceId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<LogEntry> errorLogs = logEntryRepository.findAll(spec);
        if (errorLogs.size() > MAX_ERROR_SIGNATURE_ROWS) {
            errorLogs = errorLogs.subList(0, MAX_ERROR_SIGNATURE_ROWS);
        }

        Map<String, List<LogEntry>> bySig = errorLogs.stream()
                .collect(Collectors.groupingBy(e -> normalizeSignature(e.getMessage())));

        long total = Math.max(1L, errorLogs.size());

        return bySig.entrySet().stream()
                .map(en -> {
                    List<LogEntry> rows = en.getValue();
                    long minTs = rows.stream().mapToLong(e -> e.getTimestamp().toEpochMilli()).min().orElse(0L);
                    long maxTs = rows.stream().mapToLong(e -> e.getTimestamp().toEpochMilli()).max().orElse(0L);
                    return ErrorSignatureDto.builder()
                            .signature(en.getKey())
                            .count(rows.size())
                            .percentage(round(rows.size() * 100.0 / total, 2))
                            .firstOccurrenceEpochMillis(minTs)
                            .lastOccurrenceEpochMillis(maxTs)
                            .exampleMessage(rows.get(0).getMessage())
                            .exampleStackTrace(rows.get(0).getStackTrace())
                            .build();
                })
                .sorted(Comparator.comparingLong(ErrorSignatureDto::getCount).reversed())
                .collect(Collectors.toList());
    }

    private TraceAnalyticsDto buildTracesBounded(Instant fromNorm, Instant toNorm, String serviceName,
            String category, String traceId) {
        Specification<LogEntry> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.between(root.get("timestamp"), fromNorm, toNorm));
            predicates.add(cb.isNotNull(root.get("traceId")));
            if (serviceName != null) predicates.add(cb.equal(cb.lower(root.get("serviceName")), serviceName.toLowerCase()));
            if (category != null) predicates.add(cb.equal(cb.upper(root.get("category")), category.toUpperCase()));
            if (traceId != null) predicates.add(cb.equal(root.get("traceId"), traceId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<LogEntry> traceLogs = logEntryRepository.findAll(spec);
        Map<String, List<LogEntry>> byTrace = traceLogs.stream()
                .collect(Collectors.groupingBy(LogEntry::getTraceId));

        if (byTrace.isEmpty()) {
            return TraceAnalyticsDto.builder().uniqueTraces(0).build();
        }

        List<Double> durations = new ArrayList<>();
        long failed = 0;
        for (Map.Entry<String, List<LogEntry>> en : byTrace.entrySet()) {
            List<LogEntry> rows = en.getValue();
            long minTs = rows.stream().mapToLong(e -> e.getTimestamp().toEpochMilli()).min().orElse(0L);
            long maxTs = rows.stream().mapToLong(e -> e.getTimestamp().toEpochMilli()).max().orElse(0L);
            durations.add((double) (maxTs - minTs));
            if (rows.stream().anyMatch(e -> isError(e.getLogLevel()))) {
                failed++;
            }
        }
        double[] arr = durations.stream().mapToDouble(v -> v).toArray();

        return TraceAnalyticsDto.builder()
                .uniqueTraces(byTrace.size())
                .averageLogsPerTrace(round(traceLogs.size() / (double) byTrace.size(), 2))
                .averageDurationMs(round(mean(arr), 2))
                .medianDurationMs(round(percentile(arr, 50), 2))
                .p95DurationMs(round(percentile(arr, 95), 2))
                .p99DurationMs(round(percentile(arr, 99), 2))
                .failedTraces(failed)
                .traceErrorRatePercentage(round(failed * 100.0 / byTrace.size(), 2))
                .build();
    }

    private Map<String, UserAnalyticsDto> buildUsersAggregated(Instant fromNorm, Instant toNorm, String serviceName,
            String category, String traceId) {
        List<Object[]> rows = logEntryRepository.aggregateUsers(fromNorm, toNorm, serviceName, category, traceId);
        Map<String, UserAnalyticsDto> out = new LinkedHashMap<>();

        for (Object[] r : rows) {
            String uid = (String) r[0];
            String email = (String) r[1];
            long total = ((Number) r[2]).longValue();
            long errCount = r[3] != null ? ((Number) r[3]).longValue() : 0L;

            List<Long> lats = logEntryRepository.findLatencies(fromNorm, toNorm, serviceName, category, traceId, null, null, null, null);

            out.put(uid, UserAnalyticsDto.builder()
                    .userId(uid)
                    .userEmail(email)
                    .totalLogs(total)
                    .errorCount(errCount)
                    .errorRatePercentage(round(errCount * 100.0 / Math.max(1, total), 2))
                    .averageLatencyMs(round(avg(lats), 2))
                    .p95LatencyMs(round(percentile(lats, 95), 2))
                    .build());
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Utilities
    // ------------------------------------------------------------------

    private Instant normalizeFrom(Instant from) {
        return from != null ? from : Instant.now().minus(24, ChronoUnit.HOURS);
    }

    private Instant normalizeTo(Instant to) {
        return to != null ? to : Instant.now();
    }

    private Duration resolveGranularity(String granularity) {
        if (granularity == null || granularity.isBlank()) {
            return Duration.ofHours(1);
        }
        try {
            return Duration.parse(granularity);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid granularity: " + granularity);
        }
    }

    private String cleanFilter(String filter) {
        return filter != null && !filter.isBlank() && !"ALL".equalsIgnoreCase(filter.trim()) ? filter.trim() : null;
    }

    private String normLevel(String level) {
        return level == null || level.isBlank() ? "INFO" : level.trim().toUpperCase();
    }

    private boolean isError(String level) {
        String l = normLevel(level);
        return "ERROR".equals(l) || "FATAL".equals(l);
    }

    private long countStatusRange(Map<Integer, Long> statusCounts, int lo, int hi) {
        return statusCounts.entrySet().stream()
                .filter(en -> en.getKey() >= lo && en.getKey() <= hi)
                .mapToLong(Map.Entry::getValue)
                .sum();
    }

    private String normalizeSignature(String message) {
        if (message == null) return "<null>";
        String s = message.trim().toLowerCase();
        s = s.replaceAll("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "<uuid>");
        s = s.replaceAll("\\b[0-9a-f]{10,}\\b", "<hex>");
        s = s.replaceAll("\\b\\d+\\b", "<num>");
        s = s.replaceAll("\\s+", " ").trim();
        return s.length() > 512 ? s.substring(0, 512) : s;
    }

    private Instant alignStart(Instant t, Duration bucket) {
        long epochMillis = t.toEpochMilli();
        long bucketMillis = Math.max(1L, bucket.toMillis());
        return Instant.ofEpochMilli((epochMillis / bucketMillis) * bucketMillis);
    }

    private Instant alignEnd(Instant t, Duration bucket) {
        return alignStart(t, bucket);
    }

    private double avg(List<Long> values) {
        if (values == null || values.isEmpty()) return 0.0;
        return values.stream().mapToLong(v -> v).average().orElse(0.0);
    }

    private double min(List<Long> values) {
        return values != null && !values.isEmpty() ? values.stream().mapToLong(v -> v).min().orElse(0L) : 0.0;
    }

    private double max(List<Long> values) {
        return values != null && !values.isEmpty() ? values.stream().mapToLong(v -> v).max().orElse(0L) : 0.0;
    }

    private double stddev(List<Long> values, double mean) {
        if (values == null || values.size() < 2) return 0.0;
        double sumSq = values.stream().mapToDouble(v -> (v - mean) * (v - mean)).sum();
        return Math.sqrt(sumSq / (values.size() - 1));
    }

    private double percentile(List<Long> values, double p) {
        if (values == null || values.isEmpty()) return 0.0;
        double[] sorted = values.stream().mapToDouble(v -> v).sorted().toArray();
        return percentile(sorted, p);
    }

    private double percentile(double[] sorted, double p) {
        if (sorted.length == 0) return 0.0;
        if (sorted.length == 1) return sorted[0];
        double rank = (p / 100.0) * (sorted.length - 1);
        int lower = (int) Math.floor(rank);
        int upper = (int) Math.ceil(rank);
        if (lower == upper) return sorted[lower];
        double weight = rank - lower;
        return sorted[lower] * (1 - weight) + sorted[upper] * weight;
    }

    private double mean(double[] values) {
        if (values.length == 0) return 0.0;
        double sum = 0.0;
        for (double v : values) sum += v;
        return sum / values.length;
    }

    private double round(double value, int places) {
        double factor = Math.pow(10, places);
        return Math.round(value * factor) / factor;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
