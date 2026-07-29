package com.regisx001.dQul.compute.spark.demo;

import com.regisx001.dQul.compute.spark.SparkProvider;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/spark/demo")
@RequiredArgsConstructor
public class SparkDemoController {

    private final SparkProvider sparkProvider;

    /**
     * Endpoint to check SparkSession status and metadata.
     */
    @GetMapping("/status")
    public ResponseEntity<SparkStatusResponse> getStatus() {
        SparkSession session = sparkProvider.getSparkSession();
        boolean active = sparkProvider.isSparkActive();

        log.info("==================================================");
        log.info("Spark Demo: Checking SparkSession status...");
        log.info("Active Status : {}", active);
        if (session != null) {
            log.info("App Name      : {}", session.sparkContext().appName());
            log.info("Master        : {}", session.sparkContext().master());
            log.info("App ID        : {}", session.sparkContext().applicationId());
            log.info("Version       : {}", session.version());
        }
        log.info("==================================================");

        SparkStatusResponse response = SparkStatusResponse.builder()
                .active(active)
                .appName(session != null ? session.sparkContext().appName() : "N/A")
                .master(session != null ? session.sparkContext().master() : "N/A")
                .applicationId(session != null ? session.sparkContext().applicationId() : "N/A")
                .version(session != null ? session.version() : "N/A")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to process input text using Apache Spark RDD & DataFrame API for word count.
     */
    @PostMapping("/wordcount")
    public ResponseEntity<Map<String, Object>> runWordCountDemo(@RequestBody(required = false) WordCountRequest request) {
        String textToProcess = (request != null && request.getText() != null && !request.getText().isBlank())
                ? request.getText()
                : "Spark is a unified engine for data processing. Apache Spark provides high-level APIs in Java, Scala, Python and R.";

        log.info("==================================================");
        log.info("Spark Demo: Running Word Count Pipeline");
        log.info("Input Text: {}", textToProcess);
        log.info("==================================================");

        SparkSession session = sparkProvider.getSparkSession();

        Dataset<Row> df = session.createDataset(Collections.singletonList(textToProcess), Encoders.STRING()).toDF("value");

        Dataset<Row> wordCounts = df
                .select(functions.explode(functions.split(functions.col("value"), "\\s+")).as("word"))
                .select(functions.lower(functions.regexp_replace(functions.col("word"), "[^a-zA-Z0-9]", "")).as("word"))
                .filter(functions.col("word").notEqual(""))
                .groupBy("word")
                .count()
                .orderBy(functions.col("count").desc());

        List<Row> rows = wordCounts.collectAsList();

        List<Map<String, Object>> resultList = rows.stream().map(r -> {
            Map<String, Object> item = new HashMap<>();
            item.put("word", r.getString(0));
            item.put("count", r.getLong(1));
            return item;
        }).collect(Collectors.toList());

        log.info("--- Spark Word Count Results (Total Unique Words: {}) ---", resultList.size());
        resultList.forEach(item -> log.info("Word: {:<15} | Count: {}", item.get("word"), item.get("count")));
        log.info("==================================================");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("totalUniqueWords", resultList.size());
        response.put("sparkAppId", session.sparkContext().applicationId());
        response.put("results", resultList);

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to execute a sample Spark SQL query.
     */
    @GetMapping("/sql")
    public ResponseEntity<Map<String, Object>> runSqlDemo() {
        log.info("==================================================");
        log.info("Spark Demo: Running Spark SQL Query");
        log.info("==================================================");

        String sqlQuery = "SELECT 'dQul-Spark Engine' AS engine, '3.5.5' AS version, current_timestamp() AS executionTime";
        Dataset<Row> resultDf = sparkProvider.executeSql(sqlQuery);

        List<Row> rows = resultDf.collectAsList();
        Row row = rows.get(0);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("engine", row.getString(0));
        response.put("version", row.getString(1));
        response.put("executionTime", row.get(2).toString());

        log.info("Spark SQL Output: Engine={}, Version={}, ExecutionTime={}",
                row.getString(0), row.getString(1), row.get(2));
        log.info("==================================================");

        return ResponseEntity.ok(response);
    }

    @Data
    @Builder
    public static class SparkStatusResponse {
        private boolean active;
        private String appName;
        private String master;
        private String applicationId;
        private String version;
    }

    @Data
    public static class WordCountRequest {
        private String text;
    }
}
