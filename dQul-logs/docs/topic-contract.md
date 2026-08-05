# `platform-logs-topic` — Message Contract

This document is the **single source of truth** for producing and consuming log events on the
`platform-logs-topic`. Producers (internal Data Quality Platform services) MUST emit events
matching this schema; the `dQul-logs` consumer validates and normalizes before persisting.

## Overview

| Attribute | Value |
|-----------|-------|
| Topic | `platform-logs-topic` |
| Partitions | 3 |
| Replication factor | 1 (dev/standalone) |
| **Key** | `traceId` (String) — enables partitioning + ordering per trace |
| **Value** | JSON object (schema below) |
| Delivery semantics | at-least-once — consumers should tolerate rare duplicates |
| Poison messages | routed to `platform-logs-topic.DLT` |

## Value schema (JSON)

```jsonc
{
  "traceId": "string, optional — used as the record key",
  "serviceName": "string, optional — defaults to 'unknown-service'",
  "logLevel": "string, optional — one of TRACE|DEBUG|INFO|WARN|ERROR|FATAL (case-insensitive), defaults to INFO",
  "category": "string, optional — free-form, uppercased, max 32 chars, defaults to 'INTERNAL_LOG'",
  "message": "string, REQUIRED — must not be blank",
  "stackTrace": "string, optional",
  "path": "string, optional",
  "httpMethod": "string, optional",
  "statusCode": "integer, optional",
  "executionTimeMs": "integer, optional",
  "userId": "string, optional",
  "userEmail": "string, optional",
  "metadata": "string, optional — free-form JSON string",
  "timestamp": "ISO-8601 instant, optional — defaults to ingestion time (consumer side)"
}
```

### Field details

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `traceId` | string | no | Used as the record **key**. Max 64 chars. |
| `serviceName` | string | no | Trimmed; blank → `unknown-service`. Max 64 chars. |
| `logLevel` | string | no | Whitelist: `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `FATAL` (case-insensitive). Invalid → **DLT**. Blank → `INFO`. |
| `category` | string | no | Uppercased. Max 32 chars (longer → **DLT**). Blank → `INTERNAL_LOG`. |
| `message` | string | **yes** | Must not be null/blank. Missing → **DLT**. |
| `stackTrace` | string | no | Free text. |
| `path` | string | no | Max 512 chars. |
| `httpMethod` | string | no | Max 16 chars. |
| `statusCode` | integer | no | HTTP status code. |
| `executionTimeMs` | integer | no | Request/processing latency. |
| `userId` | string | no | Max 128 chars. |
| `userEmail` | string | no | Max 128 chars. |
| `metadata` | string | no | Free-form JSON as a string. |
| `timestamp` | string (ISO-8601) | no | Instant. Blank → consumer sets `now`. |

> **Type headers:** the consumer is configured with `spring.json.use.type.headers=false` and a
> default type of `com.regisx001.dQul.logs.dto.LogIngestionDto`. Producers do NOT need to send
> `__TypeId__` headers — plain JSON is sufficient. Extra/unknown fields are tolerated and ignored.

## Example event

```json
{
  "traceId": "8f14e45f-ceea-4b7b-9e5b-4b6f9f0b8a1c",
  "serviceName": "dQul-validation",
  "logLevel": "ERROR",
  "category": "VALIDATION",
  "message": "Validation failed: 42 records violated 'not_null' on column customer_id",
  "stackTrace": null,
  "path": "/api/v1/datasets/123/validate",
  "httpMethod": "POST",
  "statusCode": 200,
  "executionTimeMs": 1840,
  "userId": "u-1042",
  "userEmail": "engineer@example.com",
  "metadata": "{\"dataset\":\"sales\",\"ruleCount\":12}",
  "timestamp": "2026-08-04T12:34:56Z"
}
```

## Producing (reference — Spring Kafka)

```java
// Producer side (any internal service). Key MUST be the traceId.
kafkaTemplate.send(
    "platform-logs-topic",
    event.getTraceId(),            // key = traceId → partitioning + per-trace ordering
    event                          // LogIngestionDto-shaped POJO (or a Map)
);
```

```yaml
# Producer config
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9093}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.add.type.headers: false   # optional; consumer ignores type headers anyway
```

Manual verification (from inside the `dqul-kafka` container):

```bash
docker exec -it dqul-kafka /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic platform-logs-topic \
  --property "parse.key=true" \
  --property "key.separator=:"

trace-123:{"traceId":"trace-123","serviceName":"demo","logLevel":"INFO","category":"VALIDATION","message":"hello"}
```

## Consuming (dQul-logs behavior)

1. Value is deserialized to `LogIngestionDto` (type headers ignored, default type).
2. `LogService.saveLog` validates + normalizes per the constraints above.
3. **Valid** events are persisted to `log_entries` (PostgreSQL).
4. **Invalid** events (bad JSON, missing message, bad `logLevel`, oversized `category`) or any
   processing failure throw `LogValidationException`; the consumer error handler retries up to
   3 times (1s backoff) then routes the record to **`platform-logs-topic.DLT`**.

## Dead-letter topic

| Attribute | Value |
|-----------|-------|
| Topic | `platform-logs-topic.DLT` |
| Partitions | 3 |
| Purpose | Holds poison/undeliverable events for later inspection/replay |
| Consumers | none in the MVP |

To inspect:

```bash
docker exec -it dqul-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic platform-logs-topic.DLT \
  --from-beginning
```
