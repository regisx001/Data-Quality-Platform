package com.regisx001.dQul.compute.service;

import com.regisx001.dQul.compute.dto.ComputeJobRequest;
import com.regisx001.dQul.compute.dto.ComputeJobResult;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class SparkComputeService {

    private static final Logger log = LoggerFactory.getLogger(SparkComputeService.class);

    private final SparkSession sparkSession;

    public SparkComputeService(SparkSession sparkSession) {
        this.sparkSession = sparkSession;
    }

    public boolean isSparkActive() {
        return sparkSession != null && !sparkSession.sparkContext().isStopped();
    }

    public String getSparkVersion() {
        return isSparkActive() ? sparkSession.version() : "INACTIVE";
    }

    public ComputeJobResult executeJob(ComputeJobRequest request) {
        long startTime = System.currentTimeMillis();
        String jobId = request.getJobId() != null ? request.getJobId() : UUID.randomUUID().toString();

        try {
            log.info("Executing Spark compute job: jobId={}, type={}", jobId, request.getJobType());

            Dataset<Row> df;
            if (request.getSqlQuery() != null && !request.getSqlQuery().isBlank()) {
                df = sparkSession.sql(request.getSqlQuery());
            } else if (request.getDatasetPath() != null && !request.getDatasetPath().isBlank()) {
                String format = request.getOptions() != null && request.getOptions().containsKey("format")
                        ? request.getOptions().get("format") : "csv";
                var reader = sparkSession.read().format(format);
                if ("csv".equalsIgnoreCase(format)) {
                    reader = reader.option("header", "true").option("inferSchema", "true");
                }
                df = reader.load(request.getDatasetPath());
            } else {
                // Return default demo dataset
                df = sparkSession.range(1, 100).toDF("id");
            }

            long rowCount = df.count();
            List<Row> rows = df.takeAsList(10);
            List<Map<String, Object>> preview = new ArrayList<>();
            String[] columns = df.columns();

            for (Row row : rows) {
                Map<String, Object> map = new HashMap<>();
                for (int i = 0; i < columns.length; i++) {
                    map.put(columns[i], row.get(i));
                }
                preview.add(map);
            }

            long duration = System.currentTimeMillis() - startTime;

            return ComputeJobResult.builder()
                    .jobId(jobId)
                    .status("SUCCESS")
                    .totalRows(rowCount)
                    .executionTimeMs(duration)
                    .completedAt(Instant.now())
                    .previewRows(preview)
                    .metrics(Map.of("columnCount", columns.length, "columns", Arrays.asList(columns)))
                    .build();

        } catch (Exception e) {
            log.error("Failed to execute Spark compute job: jobId={}", jobId, e);
            return ComputeJobResult.builder()
                    .jobId(jobId)
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .completedAt(Instant.now())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }
}
