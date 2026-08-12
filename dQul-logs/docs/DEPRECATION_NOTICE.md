# Deprecation Notice & Aggregation Migration Guide

> **Module**: `dQul-logs`  
> **Version**: 2.0.0  
> **Date**: August 2026  
> **Status**: Deprecated (Legacy In-Memory / SQL Aggregations)

---

## Executive Summary

As part of the platform upgrade to distributed Spark computing, legacy in-memory and custom PostgreSQL SQL aggregation methods in `dQul-logs` have been marked **`@Deprecated(since = "2.0")`**.

All real-time streaming metrics and batch analytics calculations are now executed by **Spark Structured Streaming** and **Spark Batch Aggregations** via Kafka event pipelines.

---

## Deprecated Components

The following interfaces, classes, and REST endpoints have been marked as deprecated:

| Component Type | Component Name / Signature | Status | Replacement |
| :--- | :--- | :--- | :--- |
| **REST Endpoint** | `GET /api/v1/logs/analytics` | `@Deprecated(since = "2.0")` | `GET /api/v1/logs/batch/history` |
| **REST Endpoint** | `GET /api/v1/logs/stats` | `@Deprecated(since = "2.0")` | `GET /api/v1/logs/stream` (SSE) |
| **Interface** | `com.regisx001.dQul.logs.service.LogAnalyticsService` | `@Deprecated(since = "2.0")` | `RealtimeLogSseService` & `BatchLogMetricService` |
| **Class** | `com.regisx001.dQul.logs.service.DefaultLogAnalyticsService` | `@Deprecated(since = "2.0")` | Distributed Spark Compute Engine (`dQul-compute`) |
| **Class** | `com.regisx001.dQul.logs.service.SqlAggregatedLogAnalyticsService` | `@Deprecated(since = "2.0")` | Distributed Spark Compute Engine (`dQul-compute`) |

---

## Architectural Comparison

```
+-----------------------------------------------------------------------------------+
|                            LEGACY ARCHITECTURE (v1.0)                             |
|  Log Entry Ingestion --> PostgreSQL (log_entries) --> Custom SQL / In-Memory      |
|                                                       Aggregation Service          |
+-----------------------------------------------------------------------------------+

                                        ||
                                        \/

+-----------------------------------------------------------------------------------+
|                            SPARK ARCHITECTURE (v2.0)                              |
|                                                                                   |
|  1. REAL-TIME STREAMING:                                                          |
|     Log Streams --> Kafka (platform-logs-topic) --> Spark Structured Streaming    |
|                 --> SSE Stream (/api/v1/logs/stream)                            |
|                                                                                   |
|  2. BATCH AGGREGATIONS:                                                           |
|     Trigger (/api/v1/logs/aggregate) --> Kafka (dqul.logs.aggregate.request)    |
|     --> Spark Batch Job (dQul-compute) --> Kafka (dqul.logs.aggregate.result)    |
|     --> dQul-logs Consumer --> PostgreSQL (batch_log_metrics JSONB)               |
|     --> History Endpoint (/api/v1/logs/batch/history)                            |
+-----------------------------------------------------------------------------------+
```

---

## Endpoint Mapping Guide

### 1. Real-Time Telemetry & Tumbling Window Metrics
- **Legacy Endpoint**: `GET /api/v1/logs/stats`
- **Replacement Endpoint**: `GET /api/v1/logs/stream` (Server-Sent Events)
- **Features**: Live 5-second tumbling window throughput, error counts, latency, and window bounds calculated in real time by Spark Structured Streaming.

### 2. Historical Batch Aggregations & Analytics
- **Legacy Endpoint**: `GET /api/v1/logs/analytics`
- **Replacement Endpoint**: `GET /api/v1/logs/batch/history` & `POST /api/v1/logs/aggregate`
- **Features**: Triggers distributed Spark batch jobs that store full high-dimensional analytics payloads (`levelCounts`, `serviceCounts`, `categoryCounts`, `topErrorMessages`) as `JSONB` in the `batch_log_metrics` PostgreSQL table.

---

## Code Deprecation Example

```java
// LEGACY (Deprecated)
@Deprecated(since = "2.0", forRemoval = false)
@GetMapping("/analytics")
public ResponseEntity<LogAnalyticsDto> getAnalytics(...) {
    return ResponseEntity.ok(logAnalyticsService.analyze(request));
}

// SPARK-POWERED (Active)
@GetMapping("/batch/history")
public ResponseEntity<List<BatchLogMetricDto>> getBatchHistory(
        @RequestParam(defaultValue = "30") int limit) {
    return ResponseEntity.ok(batchLogMetricService.getRecentBatchMetrics(limit));
}
```
