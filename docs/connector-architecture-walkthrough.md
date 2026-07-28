# Datasource Connector Architecture — Walkthrough

## Overview

The connector layer is the abstraction between the Data Quality Platform and any external data source. Every connector exposes **two distinct faces**:

- **Metadata API** — native-driver-based operations (JDBC, filesystem APIs, cloud SDKs) for connection testing, dataset discovery, schema extraction, and statistics. Used by the **UI** and **metadata services**.
- **Compute API** — produces a Spark `DataFrame` for a dataset. Used by the **Validation Engine** and **profiling pipelines**.

```
                    DataSource Connector
                            │
        ┌───────────────────┴───────────────────┐
        │                                       │
 Metadata Services                      Compute Services
        │                                       │
 Native Driver (JDBC, FS, SDK)      Spark DataFrame Provider
        │                                       │
 discover datasets                   Apache Spark
 read schema                          Validation Engine
 test connection                      Profiling
 statistics                           Distributed Processing
```

The Validation Engine depends **only** on the Compute API. The UI and metadata services depend **only** on the Metadata API. A single implementation class provides both.

---

## Package Structure

```
connector/
├── package-info.java              Package-level documentation
├── ConnectorFactory.java          Central factory — creates connectors from config
├── api/                           Public API — shared abstractions
│   ├── ConnectorConfig.java       Sealed interface for all config types
│   ├── PostgresConnectorConfig.java
│   ├── CsvConnectorConfig.java
│   ├── DataSourceConnector.java   Combined interface (extends MetadataApi + ComputeApi)
│   ├── MetadataApi.java           Native-driver metadata operations
│   ├── ComputeApi.java            Spark DataFrame production
│   ├── DataReader.java            Spark Dataset<Row> producer
│   ├── ConnectionTestResult.java  Connection test outcome
│   ├── DatasetDescriptor.java     Lightweight dataset discovery info
│   ├── DatasetMetadata.java       Full schema + estimated row count
│   ├── ColumnMetadata.java        Single column definition
│   ├── DataType.java              Canonical data type enum
│   └── DatasetType.java           Dataset categorisation enum
├── postgres/
│   └── PostgresDataSourceConnector.java  (implements both APIs)
└── csv/
    └── CsvDataSourceConnector.java       (implements both APIs)
```

---

## Architecture Principles

### 1. Two Separate Dependency Boundaries

```java
// ── Metadata API ── native drivers for rich exploration ──
public interface MetadataApi {
    ConnectionTestResult testConnection();
    List<DatasetDescriptor> discoverDatasets();
    DatasetMetadata getMetadata(String datasetId);
}

// ── Compute API ── Spark DataFrame for the validation engine ──
@FunctionalInterface
public interface ComputeApi {
    DataReader createReader(String datasetId);
}
```

**Why this matters:**

- The **Validation Engine** imports `ComputeApi` — it never sees JDBC, filesystem, or any datasource-specific code.
- The **UI** imports `MetadataApi` — it never touches Spark.
- A new developer can understand the compute path without understanding metadata exploration, and vice versa.

### 2. Combined Interface for Factory Convenience

`DataSourceConnector` simply combines both:

```java
public interface DataSourceConnector extends MetadataApi, ComputeApi {
    // No additional methods — just a convenience union
}
```

This lets the `ConnectorFactory` return a single type, while consumers always depend on the narrower interface they actually need.

### 3. Connector Configurations Are Sealed

`ConnectorConfig` is a **sealed interface** — only known subtypes are permitted:

```java
public sealed interface ConnectorConfig
        permits PostgresConnectorConfig, CsvConnectorConfig {
    String datasourceName();
}
```

This gives the `ConnectorFactory` **exhaustive pattern matching** at compile time — adding a new datasource requires explicitly updating the `permits` clause, which guarantees the factory switch stays complete.

### 4. Connector Factory

The `ConnectorFactory` is the single entry point:

```java
public static DataSourceConnector createConnector(ConnectorConfig config) {
    return switch (config) {
        case PostgresConnectorConfig c -> new PostgresDataSourceConnector(c);
        case CsvConnectorConfig c      -> new CsvDataSourceConnector(c);
    };
}
```

Callers narrow to the interface they need:

```java
// UI / metadata service
MetadataApi meta = ConnectorFactory.createConnector(config);

// Validation Engine
ComputeApi compute = ConnectorFactory.createConnector(config);
```

### 5. Spark Integration via DataReader

The `DataReader` interface bridges the connector layer to Apache Spark:

```java
public interface DataReader {
    Dataset<Row> read(SparkSession spark);
}
```

The compute engine never touches JDBC, CSV parsers, or any datasource-specific API. It just asks for a `Dataset<Row>` and runs its validations on top of Spark.

### 6. Internal Responsibilities per Connector

Each connector implementation maps its native capabilities to the two faces:

```
                    PostgreSQL Connector
                           │
          ┌────────────────┴────────────────┐
          │                                 │
    MetadataApi                        ComputeApi
    (JDBC)                             (Spark JDBC)
          │                                 │
    information_schema               spark.read.format("jdbc")
    pg_catalog                      .option("dbtable", ...)
    DatabaseMetaData                .option("fetchSize", ...)
```

```
                    CSV Connector
                           │
          ┌────────────────┴────────────────┐
          │                                 │
    MetadataApi                        ComputeApi
    (Filesystem)                       (Spark CSV)
          │                                 │
    Files.size()                     spark.read.format("csv")
    Files.lines()                    .option("delimiter", ...)
    header parsing                   .option("header", ...)
    type inference                   .option("inferSchema", ...)
```

---

## Connector Implementations

### PostgreSQL Connector

**Config fields:**
- `host`, `port` (default 5432), `database`, `schema` (default `public`)
- `username`, `password`, `ssl`, `connectionTimeoutMs` (default 30s)

**MetadataApi — native JDBC:**

| Operation | Implementation |
|---|---|
| `testConnection()` | Opens a JDBC connection, calls `isValid(5)` |
| `discoverDatasets()` | Queries `DatabaseMetaData.getTables()` for `TABLE` and `VIEW` types |
| `getMetadata()` | `DatabaseMetaData.getColumns()` for schema, `pg_class.reltuples` for row estimate |

**ComputeApi — Spark JDBC:**
```java
spark.read()
    .format("jdbc")
    .option("url", config.jdbcUrl())
    .option("dbtable", "schema.table")
    .option("fetchSize", "10000")
    .option("pushDownPredicate", "true")
    .load();
```

**JDBC URL construction:**
```java
public String jdbcUrl() {
    return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
}
```

**SQL type mapping** — the connector maps PostgreSQL types to canonical `DataType` values:

| java.sql.Types | DataType |
|---|---|
| CHAR, VARCHAR, CLOB, etc. | `STRING` |
| TINYINT, SMALLINT, INTEGER | `INTEGER` |
| BIGINT | `LONG` |
| REAL, FLOAT, DOUBLE | `DOUBLE` |
| DECIMAL, NUMERIC | `DECIMAL` |
| BOOLEAN, BIT | `BOOLEAN` |
| DATE | `DATE` |
| TIMESTAMP, TIME | `TIMESTAMP` |
| BINARY, BLOB | `BINARY` |
| ARRAY | `ARRAY` |
| STRUCT, JAVA_OBJECT | `STRUCT` |

---

### CSV Connector

**Config fields:**
- `filePath`, `delimiter` (default `,`), `header` (default `true`)
- `encoding` (default `UTF-8`), `quoteChar` (default `"`), `escapeChar` (default `\`)
- `inferSchema` (default `true`)

**MetadataApi — filesystem API:**

| Operation | Implementation |
|---|---|
| `testConnection()` | Checks that the file exists and is readable |
| `discoverDatasets()` | Returns exactly one dataset — the file itself |
| `getMetadata()` | Parses the header row for column names, samples the first data row for type hints, estimates row count by line counting |

**ComputeApi — Spark CSV:**
```java
spark.read()
    .format("csv")
    .option("sep", ",")
    .option("header", "true")
    .option("inferSchema", "true")
    .option("path", "/path/to/file.csv")
    .load();
```

**Row count estimation:**
- Files < 10 MB: full scan via `Files.lines().count()`
- Files ≥ 10 MB: reads first 1 MB, counts newlines, extrapolates linearly

**Type inference** (from a single sample row):
```
"hello"     → STRING
"42"        → INTEGER
"123456789" → LONG
"3.14"      → DOUBLE
"true"      → STRING (conservative — too ambiguous from one row)
```

---

## How to Add a New Connector

Adding support for a new datasource (e.g., MongoDB) requires four steps:

### Step 1 — Create a config record

```java
// In connector/api/MongoConnectorConfig.java
package com.regisx001.dQul.connector.api;

public record MongoConnectorConfig(
        String connectionString,
        String database,
        String datasourceName
) implements ConnectorConfig {}
```

### Step 2 — Create the connector implementation

```java
// In connector/mongo/MongoDataSourceConnector.java
package com.regisx001.dQul.connector.mongo;

public class MongoDataSourceConnector implements DataSourceConnector {
    // MetadataApi: use MongoDB Java driver for collection discovery,
    //              index extraction, validation rules, etc.
    // ComputeApi:  use Spark MongoDB Connector (spark.read.format("mongodb"))
}
```

### Step 3 — Update the sealed interface

```java
// In ConnectorConfig.java — add MongoConnectorConfig to permits
public sealed interface ConnectorConfig
        permits PostgresConnectorConfig, CsvConnectorConfig, MongoConnectorConfig {
```

### Step 4 — Update the factory

```java
// In ConnectorFactory.java — add the new case
public static DataSourceConnector createConnector(ConnectorConfig config) {
    return switch (config) {
        case PostgresConnectorConfig c -> new PostgresDataSourceConnector(c);
        case CsvConnectorConfig c      -> new CsvDataSourceConnector(c);
        case MongoConnectorConfig c    -> new MongoDataSourceConnector(c);
    };
}
```

---

## Usage Example

```java
// ── Create a config ──────────────────────────────────
ConnectorConfig config = new PostgresConnectorConfig(
    "localhost", "analytics_db", "app_user", "secret", "Analytics DB");

// ── Ask the factory for a connector ──────────────────
DataSourceConnector connector = ConnectorFactory.createConnector(config);

// ── UI / metadata service uses MetadataApi ───────────
MetadataApi meta = connector;  // narrow to MetadataApi

ConnectionTestResult result = meta.testConnection();
List<DatasetDescriptor> datasets = meta.discoverDatasets();
DatasetMetadata schema = meta.getMetadata(datasets.get(0).id());

// ── Validation Engine uses ComputeApi ────────────────
ComputeApi compute = connector;  // narrow to ComputeApi

DataReader reader = compute.createReader(datasets.get(0).id());
Dataset<Row> df = reader.read(sparkSession);

// Validation logic runs on the DataFrame...
```

---

## Why This Architecture Scales

| Datasource | Metadata API (native driver) | Compute API (Spark) |
|---|---|---|
| PostgreSQL | JDBC — `information_schema`, `pg_catalog` | Spark JDBC data source |
| MySQL | JDBC — `information_schema` | Spark JDBC data source |
| SQL Server | JDBC — `INFORMATION_SCHEMA` | Spark JDBC data source |
| CSV | Filesystem — `Files.size()`, header parsing | Spark CSV data source |
| Parquet | Filesystem + Parquet metadata | Spark Parquet data source |
| MongoDB | MongoDB Java Driver — collections, indexes | Spark MongoDB Connector |
| Snowflake | Snowflake JDBC — `INFORMATION_SCHEMA` | Spark Snowflake Connector |
| Iceberg | Catalog API — table metadata | `spark.read.format("iceberg")` |

Each new datasource adds one implementation class. The Validation Engine, metadata services, and factory remain unchanged.

---

## Dependency

Apache Spark SQL (`spark-sql_2.13`) is declared in `pom.xml` with `<scope>provided</scope>` — it is expected to be available in the runtime environment (Spark cluster) and is not bundled with the platform artifact.

```xml
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-sql_2.13</artifactId>
    <version>3.5.3</version>
    <scope>provided</scope>
</dependency>
```
