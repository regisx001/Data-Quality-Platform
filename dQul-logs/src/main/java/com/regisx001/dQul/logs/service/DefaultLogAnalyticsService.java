package com.regisx001.dQul.logs.service;

import com.regisx001.dQul.logs.domain.LogEntry;
import com.regisx001.dQul.logs.dto.analytics.*;
import com.regisx001.dQul.logs.repository.LogEntryRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Default {@link LogAnalyticsService} that loads the filtered time window into
 * memory and aggregates it across the universal observability dimensions.
 *
 * @deprecated Legacy in-memory aggregation implementation. Replaced by Spark Structured
 *             Streaming (RealtimeLogSseService) and Spark Batch Aggregations (BatchLogMetricService).
 */
@Deprecated(since = "2.0", forRemoval = false)
@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultLogAnalyticsService implements LogAnalyticsService {

    /** Safety cap on rows loaded for a single analytics request. */

    //
    private static final int MAX_ROWS = 100_000;

    private final LogEntryRepository logEntryRepository;

    @Override
    @Transactional(readOnly = true)
    public LogAnalyticsDto analyze(LogAnalyticsRequest request) {
        Instant from = request.getFrom();
        Instant to = request.getTo();
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }

        List<LogEntry> logs = loadWindow(from, to, request.getServiceName(),
                request.getCategory(), request.getTraceId());

        LogAnalyticsDto.LogAnalyticsDtoBuilder out = LogAnalyticsDto.builder()
                .from(from)
                .to(to)
                .totalLogs(logs.size());

        Duration bucket = resolveGranularity(request.getGranularity());

        out.volume(buildVolume(logs, normalizeFrom(from), normalizeTo(to), bucket));
        out.levels(buildLevels(logs));
        out.services(buildServices(logs));
        out.categories(buildCategories(logs));
        out.http(buildHttp(logs));
        out.latency(buildLatency(logs));
        out.errorSignatures(buildErrorSignatures(logs));
        out.traces(buildTraces(logs));
        out.users(buildUsers(logs));

        return out.build();
    }

    // ------------------------------------------------------------------
    // Data loading
    // ------------------------------------------------------------------

    private List<LogEntry> loadWindow(Instant from, Instant to, String serviceName,
            String category, String traceId) {
        Instant fromNorm = normalizeFrom(from);
        Instant toNorm = normalizeTo(to);

        Specification<LogEntry> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.between(root.get("timestamp"), fromNorm, toNorm));
            if (serviceName != null && !serviceName.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("serviceName")), serviceName.trim().toLowerCase()));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("category")), category.trim().toUpperCase()));
            }
            if (traceId != null && !traceId.isBlank()) {
                predicates.add(cb.equal(root.get("traceId"), traceId.trim()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // Load newest first within the window, capped.
        List<LogEntry> all = logEntryRepository.findAll(spec, Pageable.unpaged()).getContent();
        if (all.size() > MAX_ROWS) {
            all = all.subList(0, MAX_ROWS);
            log.warn("Analytics window truncated to {} rows (from {}); results approximate",
                    MAX_ROWS, all.size() + 1);
        }
        return all;
    }

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
            throw new IllegalArgumentException("Invalid granularity: " + granularity
                    + " (expected ISO-8601 duration, e.g. PT1M, PT1H)");
        }
    }

    // ------------------------------------------------------------------
    // Volume
    // ------------------------------------------------------------------

    private VolumeAnalyticsDto buildVolume(List<LogEntry> logs, Instant from, Instant to, Duration bucket) {
        Instant start = alignStart(from, bucket);
        Instant end = alignEnd(to, bucket);

        TreeMap<Instant, Long> buckets = new TreeMap<>();
        for (Instant t = start; !t.isAfter(end); t = t.plus(bucket)) {
            buckets.put(t, 0L);
        }
        for (LogEntry e : logs) {
            Instant key = alignStart(e.getTimestamp(), bucket);
            buckets.compute(key, (k, v) -> v == null ? 1L : v + 1L);
        }

        long seconds = Math.max(1, Duration.between(start, end).getSeconds());
        long total = logs.size();

        List<VolumeAnalyticsDto.VolumeBucket> series = buckets.entrySet().stream()
                .map(en -> VolumeAnalyticsDto.VolumeBucket.builder()
                        .bucket(en.getKey().toString())
                        .count(en.getValue())
                        .build())
                .collect(Collectors.toList());

        long max = buckets.values().stream().mapToLong(v -> v).max().orElse(0L);
        long min = buckets.values().stream().mapToLong(v -> v).min().orElse(0L);

        return VolumeAnalyticsDto.builder()
                .logsPerSecond(round(total / (double) seconds, 3))
                .logsPerMinute(round(total / (double) seconds * 60.0, 3))
                .maxLogsInBucket(max)
                .minLogsInBucket(min)
                .timeSeries(series)
                .build();
    }

    private Instant alignStart(Instant t, Duration bucket) {
        long epochMillis = t.toEpochMilli();
        long bucketMillis = Math.max(1L, bucket.toMillis());
        return Instant.ofEpochMilli((epochMillis / bucketMillis) * bucketMillis);
    }

    private Instant alignEnd(Instant t, Duration bucket) {
        return alignStart(t, bucket);
    }

    // ------------------------------------------------------------------
    // Log levels
    // ------------------------------------------------------------------

    private LevelAnalyticsDto buildLevels(List<LogEntry> logs) {
        Map<String, Long> byLevel = new LinkedHashMap<>();
        List<String> order = List.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL");
        order.forEach(l -> byLevel.put(l, 0L));
        for (LogEntry e : logs) {
            byLevel.compute(normLevel(e.getLogLevel()), (k, v) -> v == null ? 1L : v + 1L);
        }

        long total = Math.max(1L, logs.size());
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

    private String normLevel(String level) {
        return level == null || level.isBlank() ? "INFO" : level.trim().toUpperCase();
    }

    // ------------------------------------------------------------------
    // Service analytics
    // ------------------------------------------------------------------

    private List<ServiceAnalyticsDto> buildServices(List<LogEntry> logs) {
        Map<String, List<LogEntry>> grouped = logs.stream()
                .collect(Collectors.groupingBy(e -> e.getServiceName() == null ? "unknown" : e.getServiceName()));

        return grouped.entrySet().stream()
                .map(en -> {
                    List<LogEntry> rows = en.getValue();
                    long total = rows.size();
                    long error = countLevels(rows, "ERROR");
                    long fatal = countLevels(rows, "FATAL");
                    long warn = countLevels(rows, "WARN");
                    List<Long> lats = latencies(rows);
                    return ServiceAnalyticsDto.builder()
                            .serviceName(en.getKey())
                            .totalLogs(total)
                            .logSharePercentage(round(total * 100.0 / Math.max(1, logs.size()), 2))
                            .errorCount(error + fatal)
                            .fatalCount(fatal)
                            .warnCount(warn)
                            .errorRatePercentage(round((error + fatal) * 100.0 / total, 2))
                            .averageLatencyMs(round(avg(lats), 2))
                            .p95LatencyMs(round(percentile(lats, 95), 2))
                            .p99LatencyMs(round(percentile(lats, 99), 2))
                            .maxLatencyMs(round(max(lats), 2))
                            .build();
                })
                .sorted(Comparator.comparingLong((ServiceAnalyticsDto svc) -> svc.getTotalLogs()).reversed())
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Category analytics
    // ------------------------------------------------------------------

    private List<CategoryAnalyticsDto> buildCategories(List<LogEntry> logs) {
        Map<String, List<LogEntry>> grouped = logs.stream()
                .collect(Collectors.groupingBy(e -> e.getCategory() == null ? "UNKNOWN" : e.getCategory()));

        return grouped.entrySet().stream()
                .map(en -> {
                    List<LogEntry> rows = en.getValue();
                    long total = rows.size();
                    long error = countLevels(rows, "ERROR");
                    long fatal = countLevels(rows, "FATAL");
                    List<Long> lats = latencies(rows);
                    return CategoryAnalyticsDto.builder()
                            .category(en.getKey())
                            .totalLogs(total)
                            .logSharePercentage(round(total * 100.0 / Math.max(1, logs.size()), 2))
                            .errorCount(error + fatal)
                            .fatalCount(fatal)
                            .errorRatePercentage(round((error + fatal) * 100.0 / total, 2))
                            .averageLatencyMs(round(avg(lats), 2))
                            .p95LatencyMs(round(percentile(lats, 95), 2))
                            .p99LatencyMs(round(percentile(lats, 99), 2))
                            .build();
                })
                .sorted(Comparator.comparingLong((CategoryAnalyticsDto c) -> c.getTotalLogs()).reversed())
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // HTTP analytics
    // ------------------------------------------------------------------

    private HttpAnalyticsDto buildHttp(List<LogEntry> logs) {
        Map<Integer, Long> statusCounts = new TreeMap<>();
        Map<String, Long> methodCounts = new TreeMap<>();
        Map<String, List<LogEntry>> endpointGroups = new LinkedHashMap<>();

        for (LogEntry e : logs) {
            if (e.getStatusCode() != null) {
                statusCounts.compute(e.getStatusCode(), (k, v) -> v == null ? 1L : v + 1L);
            }
            if (e.getHttpMethod() != null && !e.getHttpMethod().isBlank()) {
                methodCounts.compute(e.getHttpMethod().trim().toUpperCase(), (k, v) -> v == null ? 1L : v + 1L);
            }
            String key = endpointKey(e);
            if (key != null) {
                endpointGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
            }
        }

        long c2 = countStatusRange(statusCounts, 200, 299);
        long c3 = countStatusRange(statusCounts, 300, 399);
        long c4 = countStatusRange(statusCounts, 400, 499);
        long c5 = countStatusRange(statusCounts, 500, 599);

        long totalHttp = c2 + c3 + c4 + c5;
        if (totalHttp == 0 && !methodCounts.isEmpty()) {
            totalHttp = methodCounts.values().stream().mapToLong(Long::longValue).sum();
        }

        List<EndpointAnalyticsDto> endpoints = endpointGroups.entrySet().stream()
                .map(en -> {
                    String[] parts = en.getKey().split("\u0001", 2);
                    String method = parts[0];
                    String path = parts.length > 1 ? parts[1] : "";
                    List<Long> lats = latencies(en.getValue());
                    long err = countLevels(en.getValue(), "ERROR") + countLevels(en.getValue(), "FATAL");
                    long n = en.getValue().size();
                    return EndpointAnalyticsDto.builder()
                            .httpMethod(method)
                            .path(path)
                            .requestCount(n)
                            .errorCount(err)
                            .errorRatePercentage(round(err * 100.0 / Math.max(1, n), 2))
                            .averageLatencyMs(round(avg(lats), 2))
                            .p95LatencyMs(round(percentile(lats, 95), 2))
                            .p99LatencyMs(round(percentile(lats, 99), 2))
                            .maxLatencyMs(round(max(lats), 2))
                            .build();
                })
                .sorted(Comparator.comparingLong((EndpointAnalyticsDto e) -> e.getRequestCount()).reversed())
                .collect(Collectors.toList());

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

    private String endpointKey(LogEntry e) {
        if (e.getPath() == null) {
            return null;
        }
        String method = e.getHttpMethod() == null || e.getHttpMethod().isBlank()
                ? "ANY"
                : e.getHttpMethod().trim().toUpperCase();
        return method + "\u0001" + e.getPath();
    }

    private long countStatusRange(Map<Integer, Long> statusCounts, int lo, int hi) {
        return statusCounts.entrySet().stream()
                .filter(en -> en.getKey() >= lo && en.getKey() <= hi)
                .mapToLong(en -> en.getValue())
                .sum();
    }

    // ------------------------------------------------------------------
    // Latency analytics
    // ------------------------------------------------------------------

    private LatencyAnalyticsDto buildLatency(List<LogEntry> logs) {
        List<Long> lats = latencies(logs);
        double avg = avg(lats);
        double sd = stddev(lats, avg);
        long slow = logs.stream()
                .filter(e -> e.getExecutionTimeMs() != null && e.getExecutionTimeMs() > 1000)
                .count();
        long total = Math.max(1L, logs.size());
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

    // ------------------------------------------------------------------
    // Error signatures
    // ------------------------------------------------------------------

    private List<ErrorSignatureDto> buildErrorSignatures(List<LogEntry> logs) {
        Map<String, List<LogEntry>> bySig = logs.stream()
                .filter(e -> isError(e.getLogLevel()))
                .collect(Collectors.groupingBy(e -> normalizeSignature(e.getMessage())));

        long total = Math.max(1L, (int) bySig.values().stream().mapToLong(v -> (long) v.size()).sum());

        return bySig.entrySet().stream()
                .map(en -> {
                    List<LogEntry> rows = en.getValue();
                    long minTs = rows.stream().mapToLong(e -> e.getTimestamp().toEpochMilli()).min().orElse(0L);
                    long maxTs = rows.stream().mapToLong(e -> e.getTimestamp().toEpochMilli()).max().orElse(0L);
                    String exampleMsg = rows.get(0).getMessage();
                    String exampleStack = rows.get(0).getStackTrace();
                    return ErrorSignatureDto.builder()
                            .signature(en.getKey())
                            .count(rows.size())
                            .percentage(round(rows.size() * 100.0 / total, 2))
                            .firstOccurrenceEpochMillis(minTs)
                            .lastOccurrenceEpochMillis(maxTs)
                            .exampleMessage(exampleMsg)
                            .exampleStackTrace(exampleStack)
                            .build();
                })
                .sorted(Comparator.comparingLong((ErrorSignatureDto sig) -> sig.getCount()).reversed())
                .collect(Collectors.toList());
    }

    private boolean isError(String level) {
        String l = normLevel(level);
        return "ERROR".equals(l) || "FATAL".equals(l);
    }

    /**
     * Collapses instance-specific noise (UUIDs, hex ids, numbers, timestamps)
     * from a message so recurring errors produce a stable fingerprint.
     */
    private String normalizeSignature(String message) {
        if (message == null) {
            return "<null>";
        }
        String s = message.trim().toLowerCase();
        s = s.replaceAll("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "<uuid>");
        s = s.replaceAll("\\b[0-9a-f]{10,}\\b", "<hex>");
        s = s.replaceAll("\\b\\d+\\b", "<num>");
        s = s.replaceAll("\\s+", " ").trim();
        return s.length() > 512 ? s.substring(0, 512) : s;
    }

    // ------------------------------------------------------------------
    // Trace analytics
    // ------------------------------------------------------------------

    private TraceAnalyticsDto buildTraces(List<LogEntry> logs) {
        Map<String, List<LogEntry>> byTrace = logs.stream()
                .filter(e -> e.getTraceId() != null && !e.getTraceId().isBlank())
                .collect(Collectors.groupingBy(e -> e.getTraceId()));

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
                .averageLogsPerTrace(round(logs.stream().filter(e -> e.getTraceId() != null).count()
                        / (double) byTrace.size(), 2))
                .averageDurationMs(round(mean(arr), 2))
                .medianDurationMs(round(percentile(arr, 50), 2))
                .p95DurationMs(round(percentile(arr, 95), 2))
                .p99DurationMs(round(percentile(arr, 99), 2))
                .failedTraces(failed)
                .traceErrorRatePercentage(round(failed * 100.0 / byTrace.size(), 2))
                .build();
    }

    // ------------------------------------------------------------------
    // User analytics
    // ------------------------------------------------------------------

    private Map<String, UserAnalyticsDto> buildUsers(List<LogEntry> logs) {
        Map<String, List<LogEntry>> grouped = logs.stream()
                .filter(e -> e.getUserId() != null && !e.getUserId().isBlank())
                .collect(Collectors.groupingBy(e -> e.getUserId()));

        Map<String, UserAnalyticsDto> out = new LinkedHashMap<>();
        grouped.forEach((uid, rows) -> {
            long error = countLevels(rows, "ERROR") + countLevels(rows, "FATAL");
            List<Long> lats = latencies(rows);
            out.put(uid, UserAnalyticsDto.builder()
                    .userId(uid)
                    .userEmail(rows.get(0).getUserEmail())
                    .totalLogs(rows.size())
                    .errorCount(error)
                    .errorRatePercentage(round(error * 100.0 / rows.size(), 2))
                    .averageLatencyMs(round(avg(lats), 2))
                    .p95LatencyMs(round(percentile(lats, 95), 2))
                    .build());
        });
        return out;
    }

    // ------------------------------------------------------------------
    // Numeric helpers
    // ------------------------------------------------------------------

    private List<Long> latencies(List<LogEntry> rows) {
        return rows.stream()
                .map(e -> e.getExecutionTimeMs())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private long countLevels(List<LogEntry> rows, String level) {
        return rows.stream().filter(e -> level.equalsIgnoreCase(normLevel(e.getLogLevel()))).count();
    }

    private double avg(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        return values.stream().mapToLong(v -> v).average().orElse(0.0);
    }

    private double min(List<Long> values) {
        return values.stream().mapToLong(v -> v).min().orElse(0L);
    }

    private double max(List<Long> values) {
        return values.stream().mapToLong(v -> v).max().orElse(0L);
    }

    private double stddev(List<Long> values, double mean) {
        if (values == null || values.size() < 2) {
            return 0.0;
        }
        double sumSq = values.stream().mapToDouble(v -> {
            double d = v - mean;
            return d * d;
        }).sum();
        return Math.sqrt(sumSq / (values.size() - 1));
    }

    private double percentile(List<Long> values, double p) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        double[] sorted = values.stream().mapToDouble(v -> v).sorted().toArray();
        return percentile(sorted, p);
    }

    private double percentile(double[] sorted, double p) {
        if (sorted.length == 0) {
            return 0.0;
        }
        if (sorted.length == 1) {
            return sorted[0];
        }
        double rank = (p / 100.0) * (sorted.length - 1);
        int lower = (int) Math.floor(rank);
        int upper = (int) Math.ceil(rank);
        if (lower == upper) {
            return sorted[lower];
        }
        double weight = rank - lower;
        return sorted[lower] * (1 - weight) + sorted[upper] * weight;
    }

    private double mean(double[] values) {
        if (values.length == 0) {
            return 0.0;
        }
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    private double round(double value, int places) {
        double factor = Math.pow(10, places);
        return Math.round(value * factor) / factor;
    }
}
