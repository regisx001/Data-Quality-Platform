# Environment Configuration Guide

This directory documents the environment configuration management for the **dQul Data Quality Platform**, detailing the separation between **Local Development Mode (`.env.dev`)** and **Production / Docker Cluster Mode (`.env.prod`)**.

---

## 🎯 Architecture Overview

The platform supports two distinct runtime environments:

```
                          ┌─────────────────────────────────────────┐
                          │         LOCAL DEVELOPMENT MODE          │
                          │              (.env.dev)                 │
                          │                                         │
                          │  • Spark: Embedded local[*]             │
                          │  • App: Runs on Host OS (IDE/run.sh)    │
                          │  • MinIO: http://localhost:21001        │
                          │  • Kafka: localhost:9093                │
                          │  • Redis: localhost:7379                │
                          └─────────────────────────────────────────┘

                                               vs

                          ┌─────────────────────────────────────────┐
                          │       PRODUCTION / CLUSTER MODE         │
                          │              (.env.prod)                │
                          │                                         │
                          │  • Spark: Standalone Cluster            │
                          │    (spark://spark-master:7077)          │
                          │  • App: Containerized in Docker         │
                          │  • MinIO: http://minio:9000             │
                          │  • Kafka: kafka:9092                    │
                          │  • Redis: redis:6379                    │
                          └─────────────────────────────────────────┘
```

---

## 📊 Environment Comparison Table

| Property Key | Local Development (`.env.dev`) | Production / Cluster (`.env.prod`) | Description |
| :--- | :--- | :--- | :--- |
| `SPARK_MASTER` | `local[*]` | `spark://spark-master:7077` | Spark Master connection URL |
| `SPARK_DRIVER_HOST` | `127.0.0.1` | `dqul-compute` | Driver network hostname |
| `SPARK_DRIVER_BIND_ADDRESS` | `127.0.0.1` | `0.0.0.0` | Driver bind IP address |
| `MINIO_ENDPOINT` | `http://localhost:21001` | `http://minio:9000` | S3 / MinIO API endpoint |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9093` | `kafka:9092` | Kafka broker connection string |
| `REDIS_HOST` | `localhost` | `redis` | Redis server hostname |
| `REDIS_PORT` | `7379` | `6379` | Redis server port |
| `DQUL_DATABASE_URL` | `jdbc:postgresql://localhost:3452/dqul` | `jdbc:postgresql://postgres:5432/dqul` | Primary PostgreSQL database URL |
| `DQUL_LOGS_DATABASE_URL` | `jdbc:postgresql://localhost:3452/dqul_logs` | `jdbc:postgresql://postgres:5432/dqul_logs` | Logs PostgreSQL database URL |

---

## 🛠️ How to Switch Environments

### 1. Local Development (`.env.dev`)
Use this mode when developing `dQul-compute` locally on your Host OS using Maven, an IDE, or `./run.sh`:

```bash
# Copy development settings to active .env
cp .env.dev .env

# Run dQul-compute locally
cd dQul-compute
./run.sh
```

---

### 2. Production / Docker Cluster (`.env.prod`)
Use this mode to run the full microservices stack inside Docker Compose on `dqul-network`:

```bash
# Copy production settings to active .env
cp .env.prod .env

# Launch full stack using production environment file
docker compose --env-file .env.prod up -d --build
```

---

## 🔍 Spark & S3 Configuration Details

### S3A Hadoop FileSystem Properties
Both environments automatically configure Apache Spark's S3A client in `SparkConfig.java`:

- `spark.hadoop.fs.s3a.impl` -> `org.apache.hadoop.fs.s3a.S3AFileSystem`
- `spark.hadoop.fs.s3a.endpoint` -> `${MINIO_ENDPOINT}`
- `spark.hadoop.fs.s3a.access.key` -> `${MINIO_ROOT_USER}`
- `spark.hadoop.fs.s3a.secret.key` -> `${MINIO_ROOT_PASSWORD}`
- `spark.hadoop.fs.s3a.path.style.access` -> `true`
- `spark.hadoop.fs.s3a.connection.ssl.enabled` -> `false`
- `spark.hadoop.fs.s3a.aws.credentials.provider` -> `org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider`

---

## 🚨 Troubleshooting Common Issues

1. **`ClassNotFoundException: Class org.apache.hadoop.fs.s3a.S3AFileSystem not found`**:
   - Ensure `Dockerfile.spark` has downloaded `hadoop-aws-3.3.4.jar` and `aws-java-sdk-bundle-1.12.262.jar` with `chmod 644` permissions in `/opt/spark/jars/`.

2. **`IllegalAccessError: sun.nio.ch.DirectBuffer` on Java 21**:
   - Verify `JAVA_TOOL_OPTIONS` includes `--add-opens` flags for Java 21 internal module access.

3. **`S3A connection timeout / hang`**:
   - Verify `MINIO_ENDPOINT` matches your execution context (`http://localhost:21001` for Host OS, `http://minio:9000` for Docker container).
