package com.regisx001.dQul.compute.engine.batch.logs;

import com.regisx001.dQul.compute.dto.logs.LogsAggregationRequest;
import com.regisx001.dQul.compute.dto.logs.LogsAggregationResultDto;
import com.regisx001.dQul.compute.engine.batch.logs.impl.SparkLogsAggregatorEngineImpl;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SparkLogsAggregatorEngineTest {

    private static SparkSession spark;
    private static LogsAggregatorEngine engine;

    @BeforeAll
    static void setupSpark() {
        spark = SparkSession.builder()
                .master("local[*]")
                .appName("SparkLogsAggregatorEngineTest")
                .config("spark.ui.enabled", "false")
                .getOrCreate();

        engine = new SparkLogsAggregatorEngineImpl();
    }

    @AfterAll
    static void tearDownSpark() {
        if (spark != null) {
            spark.stop();
        }
    }

    @Test
    void testLogsAggregation() {
        StructType schema = new StructType(new StructField[]{
                new StructField("service_name", DataTypes.StringType, false, Metadata.empty()),
                new StructField("log_level", DataTypes.StringType, false, Metadata.empty()),
                new StructField("category", DataTypes.StringType, false, Metadata.empty()),
                new StructField("message", DataTypes.StringType, true, Metadata.empty()),
                new StructField("execution_time_ms", DataTypes.LongType, true, Metadata.empty()),
                new StructField("timestamp", DataTypes.StringType, true, Metadata.empty())
        });

        List<Row> rows = Arrays.asList(
                RowFactory.create("auth-service", "INFO", "SECURITY", "User logged in", 120L, "2026-08-12T01:00:00Z"),
                RowFactory.create("auth-service", "ERROR", "SECURITY", "Invalid credentials", 50L, "2026-08-12T01:05:00Z"),
                RowFactory.create("dataset-service", "INFO", "DATASET", "Dataset created", 450L, "2026-08-12T01:10:00Z"),
                RowFactory.create("dataset-service", "WARN", "DATASET", "High memory usage", 800L, "2026-08-12T01:15:00Z"),
                RowFactory.create("auth-service", "ERROR", "SECURITY", "Invalid credentials", 45L, "2026-08-12T01:20:00Z")
        );

        Dataset<Row> df = spark.createDataFrame(rows, schema);

        UUID jobId = UUID.randomUUID();
        LogsAggregationRequest request = LogsAggregationRequest.builder()
                .jobId(jobId)
                .build();

        LogsAggregationResultDto result = engine.aggregate(request, df);

        assertNotNull(result);
        assertEquals(jobId, result.getJobId());
        assertEquals(5L, result.getTotalLogsCount());

        // Level counts
        assertEquals(2L, result.getLevelCounts().get("INFO"));
        assertEquals(2L, result.getLevelCounts().get("ERROR"));
        assertEquals(1L, result.getLevelCounts().get("WARN"));

        // Service counts
        assertEquals(3L, result.getServiceCounts().get("auth-service"));
        assertEquals(2L, result.getServiceCounts().get("dataset-service"));

        // Execution time metrics
        assertNotNull(result.getAvgExecutionTimeMs());
        assertEquals(293.0, result.getAvgExecutionTimeMs(), 0.1);
        assertEquals(800L, result.getMaxExecutionTimeMs());

        // Error summary
        assertFalse(result.getTopErrorMessages().isEmpty());
        LogsAggregationResultDto.ErrorLogSummaryDto topError = result.getTopErrorMessages().get(0);
        assertEquals("auth-service", topError.getServiceName());
        assertEquals("Invalid credentials", topError.getMessage());
        assertEquals(2L, topError.getCount());
    }
}
