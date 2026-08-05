# dQul-logs

Logs microservice for the Data Quality Platform — a Spring Boot 3.4.2 / Java 21 service that
**ingests log events natively over Kafka**, persists them to PostgreSQL, and exposes a
read/ops-only REST API for querying, stats, and retention purge.

> **Standalone by design**
> This service is **not** registered as a module of the main `dQul` application and remains
> independently deployable. It shares platform infrastructure (Kafka, PostgreSQL) defined in the
> root `docker-compose.yaml`. HTTP log ingestion is deferred to a post-MVP hybrid phase — the
> write path is Kafka-only.

## Architecture

```mermaid
graph LR
    P[Internal producers<br/>(platform services)] -->|produce JSON event, key = traceId| T[(Kafka<br/>platform-logs-topic)]
    T --> C[KafkaLogConsumer]
    C -->|validate + persist| DB[(PostgreSQL<br/>dqul_logs)]
    DB --> API[Read/ops REST API<br/>:7001 /api/v1/logs]
```

- **Ingestion:** Kafka-native. Producers publish log events to `platform-logs-topic`
  (key = `traceId`). No HTTP on the write path.
- **Persistence:** `KafkaLogConsumer` validates, normalizes, and stores events in
  `log_entries` (PostgreSQL, Flyway-managed schema).
- **Poison messages:** bad events are routed to `platform-logs-topic.DLT`.
- **Read API:** query, get-by-id, stats, purge — no write endpoints in the MVP.

## Requirements

- JDK 21
- Docker (for the standalone Kafka + Postgres)

## Running locally (standalone)

From the repository root:

```bash
# 1. Start shared infra: Postgres (dqul-postgres) + Kafka
#    (DB is already configured; Kafka is defined in the root docker-compose.yaml)
docker compose up -d

# 2. (optional) copy env overrides from the root template
cp .env.example .env

# 3. Run the service
cd dQul-logs
./mvnw spring-boot:run
# or
./run.sh
```

The read API is available at `http://localhost:7001/api/v1/logs` and Swagger docs at
`http://localhost:7001/swagger-ui.html`.

## Producing a log event (Kafka)

Producers write JSON events to `platform-logs-topic` with the message key set to `traceId`.
See [docs/topic-contract.md](docs/topic-contract.md) for the full schema.

Quick manual check from inside the Kafka container:

```bash
docker exec -it dqul-kafka /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9093 \
  --topic platform-logs-topic \
  --property "parse.key=true" \
  --property "key.separator=:"

trace-123:{"traceId":"trace-123","serviceName":"demo-service","logLevel":"INFO","category":"VALIDATION","message":"hello from kafka producer"}
```

Then query it via the API:

```bash
curl "http://localhost:7001/api/v1/logs?serviceName=demo-service"
```

## REST API (read/ops)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/logs` | Query logs (filters: `search`, `level`, `serviceName`, `category`, `traceId`; pagination: `page`, `size`) |
| GET | `/api/v1/logs/{id}` | Fetch a single log entry |
| GET | `/api/v1/logs/stats` | Aggregate stats (totals, error rate, avg latency, by service/category) |
| DELETE | `/api/v1/logs/purge?days=30` | Purge logs older than N days (1–365) |

## Config

All configuration is env-var driven with sensible defaults (see `application.yaml` and
`.env.example`).

| Variable | Default | Purpose |
|----------|---------|---------|
| `DQUL_LOGS_DATABASE_URL` | `jdbc:postgresql://localhost:3452/dqul_logs` | PostgreSQL JDBC URL |
| `DQUL_LOGS_DATABASE_USERNAME` | `postgres` | DB user |
| `DQUL_LOGS_DATABASE_PASSWORD` | `postgres` | DB password |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9093` | Kafka broker address (host listener) |
| `SERVER_PORT` | `7001` | REST API port |
| `LOGS_PURGE_DEFAULT_DAYS` | `30` | Default purge retention (days) |

## Tests

```bash
./mvnw test
```

## Containerization

```bash
docker build -t dqul-logs .
```
