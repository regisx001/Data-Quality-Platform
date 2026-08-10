# Spark Wiring Guide: Embedded Mode (`local[*]`) vs Standalone Docker Cluster Mode

This guide explains how to configure, switch, and troubleshoot Apache Spark 3.5 in the `dQul-compute` microservice across two operational modes:

1. **Embedded / Local Mode (`local[*]`)** — Runs Driver and Executors in the microservice JVM process.
2. **Standalone Cluster Mode (`spark://localhost:7077`)** — Connects the microservice as a client driver to an external Spark cluster in Docker.

---

## Quick Comparison

| Setting | Embedded / Local Mode | Standalone Docker Cluster Mode |
|---|---|---|
| **Use Case** | Local Dev, Unit Testing, Quick Prototyping | Production, Scaling, Heavy Compute |
| `SPARK_MASTER` | `local[*]` | `spark://localhost:7077` |
| `SPARK_DRIVER_HOST` | `127.0.0.1` | `host.docker.internal` |
| `SPARK_DRIVER_BIND_ADDRESS` | `127.0.0.1` | `0.0.0.0` |
| `SPARK_UI_ENABLED` | `false` | `false` (Cluster UI runs on Docker `:8080`) |
| **Spark Master Dashboard** | N/A | **`http://localhost:8080`** |
| **Microservice REST API** | **`http://localhost:7002`** | **`http://localhost:7002`** |

---

## Mode 1: Embedded / Local Mode (`local[*]`)

In Local Mode, Spark runs directly inside your Spring Boot application JVM process. No external Docker services are required.

### 1. `.env` Configuration
```env
SPARK_ENABLED=true
SPARK_MASTER=local[*]
SPARK_DRIVER_HOST=127.0.0.1
SPARK_DRIVER_BIND_ADDRESS=127.0.0.1
SPARK_UI_ENABLED=false
```

### 2. How to Start
```bash
cd dQul-compute
./run.sh
```

### 3. Key Notes for Local Mode
- Embedded Spark Web UI is disabled (`SPARK_UI_ENABLED=false`) to prevent `javax.servlet` vs `jakarta.servlet` classloader conflicts with Spring Boot 3.
- All compute operations run within the memory allocated to your Spring Boot process (`SPARK_DRIVER_MEMORY=1g`).

---

## Mode 2: Standalone Cluster Mode (`spark://localhost:7077`)

In Cluster Mode, Spark Master and Workers run in isolated Docker containers, while `dQul-compute` acts as the Driver submitting jobs to the cluster.

### 1. `docker-compose.yaml` Setup
Ensure `spark-master` and `spark-worker` include `extra_hosts` mapping to allow worker containers to talk back to your host OS driver:

```yaml
  spark-master:
    image: apache/spark:3.5.1
    container_name: dqul-spark-master
    command: /opt/spark/bin/spark-class org.apache.spark.deploy.master.Master
    extra_hosts:
      - "host.docker.internal:host-gateway"
    ports:
      - "7077:7077"
      - "8080:8080"
    networks:
      - dqul-network

  spark-worker:
    image: apache/spark:3.5.1
    container_name: dqul-spark-worker
    command: /opt/spark/bin/spark-class org.apache.spark.deploy.worker.Worker spark://spark-master:7077
    extra_hosts:
      - "host.docker.internal:host-gateway"
    depends_on:
      - spark-master
    networks:
      - dqul-network
```

### 2. `.env` Configuration
```env
SPARK_ENABLED=true
SPARK_MASTER=spark://localhost:7077
SPARK_DRIVER_HOST=host.docker.internal
SPARK_DRIVER_BIND_ADDRESS=0.0.0.0
SPARK_UI_ENABLED=false
```

### 3. How to Start
```bash
# Step 1: Launch Spark Cluster containers
docker compose up -d spark-master spark-worker

# Step 2: Start microservice
cd dQul-compute
./run.sh
```

### 4. Dashboards & Access
- **Spark Cluster Master UI**: `http://localhost:8080` (Shows active workers, running applications, cores, memory).
- **Compute REST API**: `http://localhost:7002/api/v1/compute/health`

---

## Troubleshooting Checklist

### 🔴 Problem: `java.lang.UnsatisfiedDependencyException: sun.nio.ch.DirectBuffer`
- **Cause**: JDK 17/21 strong module encapsulation blocking Spark reflection.
- **Fix**: Verify `JAVA_TOOL_OPTIONS` in `run.sh` contains `--add-opens=java.base/sun.nio.ch=ALL-UNNAMED` and related flags.

### 🔴 Problem: `InvalidClassException: local class incompatible: stream classdesc serialVersionUID ...`
- **Cause**: Version mismatch between `dQul-compute` POM (`spark-core_2.13`) and Docker container (`apache/spark:3.5.1` compiled with Scala 2.12).
- **Fix**: Use Scala 2.12 dependencies (`spark-core_2.12` and `spark-sql_2.12` version `3.5.1`) in `pom.xml`.

### 🔴 Problem: `Executor EXITED (Command exited with code 1)` Log Loop
- **Cause**: Worker container inside Docker network cannot connect back to driver IP (`127.0.0.1`).
- **Fix**:
  1. Set `SPARK_DRIVER_HOST=host.docker.internal` in `.env`.
  2. Set `SPARK_DRIVER_BIND_ADDRESS=0.0.0.0` in `.env`.
  3. Ensure `extra_hosts: ["host.docker.internal:host-gateway"]` is in `docker-compose.yaml`.
