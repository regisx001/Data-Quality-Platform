#!/usr/bin/env font
import random
import uuid
import json
from datetime import datetime, timedelta, timezone
import psycopg2

# Database connection parameters
DB_URL = "postgresql://postgres:postgres@localhost:3452/dqul_logs"

SERVICES = [
    "dQul-api",
    "dQul-ingest",
    "dQul-validation",
    "dQul-compute",
    "dQul-connector",
    "dQul-logs",
    "spark-job"
]

CATEGORIES = [
    "API",
    "INGESTION",
    "VALIDATION",
    "COMPUTE",
    "CONNECTOR",
    "AUTH",
    "SECURITY",
    "SCHEDULER",
    "DATASOURCE"
]

LOG_LEVELS = ["TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL"]
LEVEL_WEIGHTS = [0.05, 0.10, 0.50, 0.20, 0.12, 0.03]

HTTP_METHODS = ["GET", "POST", "PUT", "DELETE", "PATCH"]
PATHS = [
    "/api/v1/datasets/102/validate",
    "/api/v1/datasets/sales_reporting",
    "/api/v1/connectors/postgres-main",
    "/api/v1/jobs/job-8812/run",
    "/api/v1/quality-checks/execution",
    "/api/v1/profiles/customer_db",
    "/api/v1/logs/analytics",
    "/api/v1/auth/login"
]

STATUS_CODES = [200, 201, 204, 301, 302, 400, 401, 403, 404, 500, 502, 503]
STATUS_WEIGHTS = [0.65, 0.08, 0.04, 0.02, 0.01,
                  0.08, 0.04, 0.02, 0.03, 0.02, 0.005, 0.005]

ERROR_MESSAGES = [
    ("connection refused to 10.0.0.5:5432", "java.net.ConnectException: Connection refused to PostgreSQL master host 10.0.0.5:5432\n\tat org.postgresql.core.v3.ConnectionFactoryImpl.openConnectionImpl(ConnectionFactoryImpl.java:319)\n\tat org.postgresql.core.ConnectionFactory.openConnection(ConnectionFactory.java:49)"),
    ("Timeout waiting for Kafka topic partition lock",
     "org.apache.kafka.common.errors.TimeoutException: Failed to acquire lock for platform-logs-topic within 5000ms\n\tat org.apache.kafka.clients.producer.KafkaProducer.doSend(KafkaProducer.java:981)"),
    ("NullPointerException in SchemaValidationEngine",
     "java.lang.NullPointerException: Cannot read field 'dataType' because 'column' is null\n\tat com.dqul.validation.engine.SchemaValidator.validateColumns(SchemaValidator.java:142)"),
    ("MinIO S3 storage bucket quota exceeded",
     "com.amazonaws.services.s3.model.AmazonS3Exception: Bucket quota reached for dataset bucket sales_raw (Service: Amazon S3; Status Code: 400)"),
    ("JWT authentication token signature mismatch",
     "io.jsonwebtoken.SignatureException: JWT signature does not match locally computed signature\n\tat io.jsonwebtoken.impl.DefaultJwtParser.parse(DefaultJwtParser.java:398)")
]

USERS = [
    ("u-1042", "engineer@example.com"),
    ("u-2091", "data-lead@example.com"),
    ("u-3301", "ops-admin@example.com"),
    ("u-8842", "analyst@example.com"),
    (None, None)
]


def generate_seed_data(num_events=500):
    now = datetime.now(timezone.utc)
    events = []

    for i in range(num_events):
        # Spread timestamps over the last 24 hours
        minutes_ago = random.randint(0, 24 * 60)
        seconds_offset = random.randint(0, 59)
        ts = now - timedelta(minutes=minutes_ago, seconds=seconds_offset)

        log_level = random.choices(LOG_LEVELS, weights=LEVEL_WEIGHTS)[0]
        service = random.choice(SERVICES)
        category = random.choice(CATEGORIES)
        trace_id = str(uuid.uuid4())
        log_id = str(uuid.uuid4())

        stack_trace = None
        if log_level in ("ERROR", "FATAL"):
            err_msg, stack_trace = random.choice(ERROR_MESSAGES)
            message = f"Critical error in {service}: {err_msg}"
            status_code = random.choice([500, 502, 503, 400])
        elif log_level == "WARN":
            message = f"High resource utilization or delayed response in {service}"
            status_code = random.choice([400, 401, 403, 404, 200])
        else:
            message = f"Operation completed successfully for service {service} in category {category}"
            status_code = random.choices(
                STATUS_CODES, weights=STATUS_WEIGHTS)[0]

        method = random.choice(HTTP_METHODS)
        path = random.choice(PATHS)
        exec_time = random.randint(
            15, 3500) if log_level != "FATAL" else random.randint(5000, 12000)

        user_id, user_email = random.choice(USERS)
        metadata_json = json.dumps({
            "environment": "production",
            "node": f"k8s-pod-{random.randint(1, 10)}",
            "attempts": random.randint(1, 3)
        })

        events.append((
            log_id,
            trace_id,
            service,
            log_level,
            category,
            message,
            stack_trace,
            path,
            method,
            status_code,
            exec_time,
            user_id,
            user_email,
            metadata_json,
            ts
        ))

    return events


def seed_database():
    print("Connecting to PostgreSQL database dqul_logs...")
    conn = psycopg2.connect(DB_URL)
    cur = conn.cursor()

    print("Generating 600 realistic log events across the last 24 hours...")
    data = generate_seed_data(600)

    insert_query = """
		INSERT INTO log_entries (
			id, trace_id, service_name, log_level, category, message, stack_trace,
			path, http_method, status_code, execution_time_ms, user_id, user_email,
			metadata, timestamp
		) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
	"""

    cur.executemany(insert_query, data)
    conn.commit()

    cur.execute("SELECT COUNT(*) FROM log_entries;")
    total = cur.fetchone()[0]
    print(
        f"Successfully seeded database! Total rows in log_entries table: {total}")

    cur.close()
    conn.close()


if __name__ == "__main__":
    seed_database()
