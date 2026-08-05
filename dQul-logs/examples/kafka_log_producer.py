#!/usr/bin/env python3
"""
kafka_log_producer.py — single-file log event generator + Kafka producer.

Generates realistic log events that conform to the ``platform-logs-topic`` message
contract (see ``dQul-logs/docs/topic-contract.md``) and publishes them to Kafka
**over time** — a continuous stream that the ``dQul-logs`` microservice consumes
and persists to PostgreSQL.

This is the only file you need. Each event is shaped like ``LogIngestionDto``:

    traceId, serviceName, logLevel, category, message, stackTrace, path,
    httpMethod, statusCode, executionTimeMs, userId, userEmail, metadata, timestamp

The Kafka message **key** is the ``traceId`` (partitioning + per-trace ordering).

Usage
-----
    pip install kafka-python                          # the only dependency

    # Stream 100 events, one per second, to the default local Kafka:
    python3 kafka_log_producer.py --count 100 --interval 1

    # Stream forever (press Ctrl+C to stop):
    python3 kafka_log_producer.py

    # Custom broker / topic / rate:
    python3 kafka_log_producer.py --bootstrap-servers localhost:9092 \
        --topic platform-logs-topic --interval 0.5

    # Only events for one service / level, deterministically:
    python3 kafka_log_producer.py --service dQul-validation --level WARN --seed 42
"""

from __future__ import annotations

import argparse
import json
import random
import sys
import time
import uuid
from datetime import datetime, timezone
from typing import Any, Dict, Iterable, Optional

DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092"
DEFAULT_TOPIC = "platform-logs-topic"

# ---------------------------------------------------------------------------
# Domain data (mirrors docs/topic-contract.md)
# ---------------------------------------------------------------------------

LOG_LEVELS = ("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL")

# logLevel -> relative frequency (INFO is the most common)
LEVEL_WEIGHTS = {"TRACE": 3, "DEBUG": 10,
                 "INFO": 45, "WARN": 20, "ERROR": 15, "FATAL": 2}

SERVICES = (
    "dQul-api",
    "dQul-ingest",
    "dQul-validation",
    "dQul-compute",
    "dQul-connector",
    "dQul-logs",
    "spark-job",
)

# category -> categories a service is most likely to emit
SERVICE_CATEGORY_BIAS = {
    "dQul-api": ("API", "AUTH", "SECURITY"),
    "dQul-ingest": ("INGESTION", "CONNECTOR", "SCHEDULER"),
    "dQul-validation": ("VALIDATION", "COMPUTE"),
    "dQul-compute": ("COMPUTE", "SCHEDULER"),
    "dQul-connector": ("CONNECTOR", "DATASOURCE"),
    "dQul-logs": ("INTERNAL_LOG", "API"),
    "spark-job": ("COMPUTE", "SCHEDULER"),
}
FALLBACK_CATEGORIES = (
    "API",
    "INGESTION",
    "VALIDATION",
    "COMPUTE",
    "CONNECTOR",
    "AUTH",
    "SECURITY",
    "SCHEDULER",
    "DATASOURCE",
    "INTERNAL_LOG",
)

HTTP_METHODS = ("GET", "POST", "PUT", "DELETE", "PATCH")

PATHS = (
    "/api/v1/datasets/{id}/validate",
    "/api/v1/datasets/{id}",
    "/api/v1/connectors",
    "/api/v1/jobs/{id}/run",
    "/api/v1/quality-checks",
    "/api/v1/profiles/{id}",
    "/api/v1/lineage",
    "/api/v1/users/{id}",
    "/health",
)

# logLevel -> weighted HTTP status codes (INFO/WARN lean 2xx, ERROR/FATAL lean 4xx/5xx)
STATUS_CODES = {
    "TRACE": (200,),
    "DEBUG": (200, 200, 201),
    "INFO": (200, 200, 200, 201, 204),
    "WARN": (200, 200, 429, 503),
    "ERROR": (400, 404, 422, 500, 500),
    "FATAL": (500, 500, 503),
}

DATASETS = ("sales", "customers", "products", "inventory",
            "transactions", "orders", "events", "sessions")

QUALITY_RULES = ("not_null", "unique", "regex", "range",
                 "type", "custom_sql", "cross_field")

CONNECTORS = ("postgres", "mysql", "s3", "kafka",
              "bigquery", "snowflake", "delta-lake")

USERS = (
    ("u-1001", "alice@example.com"),
    ("u-1002", "bob@example.com"),
    ("u-1042", "engineer@example.com"),
    ("u-1077", "data.ops@example.com"),
    ("u-2001", "admin@example.com"),
)

# logLevel -> realistic message templates (filled with generate_event's fields)
MESSAGE_TEMPLATES = {
    "TRACE": (
        "Entering method {method} on {component}",
        "Cache hit for key {key}",
        "Event loop tick {n}",
    ),
    "DEBUG": (
        "Parsed {n} rows from {dataset} in {ms}ms",
        "Spooling partition {n} to local buffer",
        "Resolved datasource alias '{connector}'",
    ),
    "INFO": (
        "Validation completed for dataset '{dataset}': {rows} rows checked, {violations} violations found",
        "Ingestion job {job_id} finished successfully ({rows} rows, {ms}ms)",
        "Profile computed for '{dataset}' across {columns} columns",
        "Connection established to datasource '{connector}'",
        "Quality score for '{dataset}': {score:.1f}%",
    ),
    "WARN": (
        "Slow query on '{dataset}' took {ms}ms (threshold {threshold}ms)",
        "Retrying connector '{connector}' (attempt {attempt}/{max})",
        "High violation rate ({rate:.1f}%) detected for '{dataset}'",
        "Disk usage above threshold on {component}: {pct}%",
    ),
    "ERROR": (
        "Validation failed: {n} records violated '{rule}' on column {column}",
        "Failed to connect to datasource '{connector}': connection refused",
        "Job {job_id} failed after {ms}ms with {n} errors",
        "Unparseable record at line {line} in '{dataset}'",
    ),
    "FATAL": (
        "Fatal error: unable to start worker for '{component}'",
        "Consumer group {group} lost partition ownership",
        "Datasource '{connector}' unreachable; pipeline {pipeline} halted",
    ),
}


class _SafeDict(dict):
    """dict that returns '' for missing keys so message templates never KeyError."""

    def __missing__(self, key: str) -> str:
        return ""


def _rng(seed: Optional[int]) -> random.Random:
    return random.Random(seed) if seed is not None else random


def _uuid4(rng: random.Random) -> uuid.UUID:
    """Generate a v4 UUID from the seeded RNG so runs are reproducible."""
    return uuid.UUID(int=rng.getrandbits(128), version=4)


def _utc_now() -> str:
    return (
        datetime.now(timezone.utc)
        .isoformat(timespec="milliseconds")
        .replace("+00:00", "Z")
    )


def _weighted(rng: random.Random, values: Iterable[str], weights: Iterable[int]) -> str:
    return rng.choices(list(values), weights=list(weights), k=1)[0]


def _fake_stack_trace(rng: random.Random) -> str:
    frames = (
        "com.regisx001.dQul.validation.Validator.check",
        "com.regisx001.dQul.connector.JdbcConnector.query",
        "org.apache.spark.sql.execution.datasources.FileFormatWriter",
        "com.regisx001.dQul.service.IngestionService.run",
    )
    return "\n\tat ".join(
        [f"java.lang.RuntimeException: {rng.choice(('boom', 'timeout', 'refused', 'npe'))}"]
        + rng.sample(frames, k=rng.randint(2, 4))
    )


# ---------------------------------------------------------------------------
# Generation
# ---------------------------------------------------------------------------

def generate_event(
    service_name: Optional[str] = None,
    log_level: Optional[str] = None,
    seed: Optional[int] = None,
) -> Dict[str, Any]:
    """Generate a single random log event conforming to the platform-logs-topic contract."""
    rng = _rng(seed)

    level = (log_level or _weighted(rng, LOG_LEVELS, [
             LEVEL_WEIGHTS[l] for l in LOG_LEVELS])).upper()
    service = service_name or rng.choice(SERVICES)

    category = rng.choice(SERVICE_CATEGORY_BIAS.get(
        service, FALLBACK_CATEGORIES))

    dataset = rng.choice(DATASETS)
    connector = rng.choice(CONNECTORS)
    user_id, user_email = rng.choice(USERS)
    http_method = rng.choice(HTTP_METHODS)
    status_code = rng.choice(STATUS_CODES[level])
    path = rng.choice(PATHS).format(id=rng.randint(1, 5000))
    execution_ms = rng.randint(1, 9000) if level in (
        "INFO", "WARN", "ERROR", "FATAL") else rng.randint(0, 200)

    # Field set used to fill the message template.
    fields = {
        "dataset": dataset,
        "connector": connector,
        "component": rng.choice(("worker", "scheduler", "ingest", "profilers", "api-gateway")),
        "column": rng.choice(("customer_id", "email", "amount", "created_at", "status", "sku")),
        "rule": rng.choice(QUALITY_RULES),
        "job_id": f"job-{rng.randint(1000, 9999)}",
        "pipeline": rng.choice(("nightly", "realtime", "backfill")),
        "group": "dqul-logs-group",
        "key": rng.choice(("dataset-cache", "profile-cache", "rule-cache")),
        "method": http_method,
        "n": rng.randint(1, 50000),
        "rows": rng.randint(100, 5_000_000),
        "violations": rng.randint(0, 2500),
        "columns": rng.randint(3, 120),
        "ms": execution_ms,
        "threshold": rng.randint(2000, 5000),
        "attempt": rng.randint(1, 3),
        "max": 3,
        "rate": rng.uniform(0.0, 25.0),
        "pct": rng.randint(60, 99),
        "line": rng.randint(1, 100000),
        "score": rng.uniform(60.0, 99.9),
    }
    message = rng.choice(MESSAGE_TEMPLATES[level]).format_map(
        _SafeDict(**fields))

    metadata = json.dumps(
        {
            "dataset": dataset,
            "rows": fields["rows"],
            "violations": fields["violations"],
            "ruleCount": rng.randint(1, 20),
            "environment": "dev",
        }
    )

    return {
        "traceId": str(_uuid4(rng)),
        "serviceName": service,
        "logLevel": level,
        "category": category,
        "message": message,
        "stackTrace": _fake_stack_trace(rng) if level in ("ERROR", "FATAL") else None,
        "path": path,
        "httpMethod": http_method,
        "statusCode": status_code,
        "executionTimeMs": execution_ms,
        "userId": user_id,
        "userEmail": user_email,
        "metadata": metadata,
        "timestamp": _utc_now(),
    }


# ---------------------------------------------------------------------------
# Kafka streaming
# ---------------------------------------------------------------------------

def _produce(
    producer: Any,
    topic: str,
    count: int,
    interval: float,
    service_name: Optional[str],
    log_level: Optional[str],
    seed: Optional[int],
) -> int:
    """Publish events to Kafka over time. Returns the number of events sent."""
    rng = random.Random(seed)  # seed=None -> nondeterministic
    sent = 0
    try:
        while count == 0 or sent < count:
            event = generate_event(
                service_name=service_name,
                log_level=log_level,
                seed=rng.randint(0, 2**31 - 1),
            )
            producer.send(topic, key=event["traceId"], value=json.dumps(event))
            sent += 1
            if sent == 1 or sent % 10 == 0 or sent == count:
                print(
                    f"{_utc_now()}  sent {sent} event(s) to '{topic}' "
                    f"({event['serviceName']}/{event['logLevel']})",
                    file=sys.stderr,
                )
            time.sleep(interval)
    except KeyboardInterrupt:
        print("\nStopped by user (Ctrl+C).", file=sys.stderr)
    return sent


def main(argv: Optional[list[str]] = None) -> int:
    parser = argparse.ArgumentParser(
        prog="kafka_log_producer.py",
        description="Stream realistic log events to Kafka over time (platform-logs-topic contract).",
    )
    parser.add_argument(
        "--bootstrap-servers", default=DEFAULT_BOOTSTRAP_SERVERS,
        help=f"Kafka bootstrap servers (default: {DEFAULT_BOOTSTRAP_SERVERS})",
    )
    parser.add_argument(
        "--topic", default=DEFAULT_TOPIC,
        help=f"Kafka topic (default: {DEFAULT_TOPIC})",
    )
    parser.add_argument(
        "--count", type=int, default=0,
        help="number of events to send; 0 = stream forever until Ctrl+C (default: 0)",
    )
    parser.add_argument(
        "--interval", type=float, default=1.0,
        help="seconds between events, may be fractional (default: 1.0)",
    )
    parser.add_argument(
        "--service", default=None,
        help="serviceName to stamp on every event (default: random)",
    )
    parser.add_argument(
        "--level", choices=LOG_LEVELS, default=None,
        help="logLevel to stamp on every event (default: random)",
    )
    parser.add_argument(
        "--seed", type=int, default=None,
        help="random seed for reproducible event content",
    )
    args = parser.parse_args(argv)

    if args.count < 0:
        parser.error("--count must be >= 0 (0 = stream forever)")
    if args.interval < 0:
        parser.error("--interval must be >= 0")

    try:
        from kafka import KafkaProducer
        try:
            from kafka.serializer import DefaultSerializer as StringSerializer  # kafka-python >= 3.0
        except ImportError:
            from kafka.serializer import StringSerializer  # kafka-python < 3.0
    except ImportError as exc:
        raise SystemExit(
            "Kafka publishing requires 'kafka-python'. Install it with:\n  pip install kafka-python"
        ) from exc

    producer = KafkaProducer(
        bootstrap_servers=args.bootstrap_servers,
        key_serializer=StringSerializer("utf-8"),
        value_serializer=StringSerializer("utf-8"),
    )
    try:
        sent = _produce(
            producer,
            args.topic,
            args.count,
            args.interval,
            args.service,
            args.level,
            args.seed,
        )
    finally:
        producer.flush()
        producer.close()

    print(
        f"Done. Flushed {sent} event(s) to Kafka topic '{args.topic}' ({args.bootstrap_servers}).",
        file=sys.stderr,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
