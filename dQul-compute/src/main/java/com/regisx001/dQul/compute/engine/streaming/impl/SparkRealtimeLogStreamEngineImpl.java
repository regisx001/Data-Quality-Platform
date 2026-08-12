package com.regisx001.dQul.compute.engine.streaming.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.regisx001.dQul.compute.dto.streaming.RealtimeLogMetricsDto;
import com.regisx001.dQul.compute.engine.streaming.RealtimeLogStreamEngine;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.streaming.OutputMode;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.Trigger;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SparkRealtimeLogStreamEngineImpl implements RealtimeLogStreamEngine {

    private static final Logger log = LoggerFactory.getLogger(SparkRealtimeLogStreamEngineImpl.class);
    public static final String REDIS_REALTIME_CHANNEL = "dqul:logs:realtime:stream";

    private final SparkSession sparkSession;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.kafka.bootstrap-servers:kafka:9092}")
    private String bootstrapServers;

    @Value("${KAFKA_TOPIC_PLATFORM_LOGS:platform-logs-topic}")
    private String logsTopic;

    private StreamingQuery activeQuery;

    public SparkRealtimeLogStreamEngineImpl(SparkSession sparkSession,
                                            StringRedisTemplate redisTemplate) {
        this.sparkSession = sparkSession;
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public synchronized void startLogStreamingQuery() {
        if (isStreamingActive()) {
            log.info("Real-time Spark Structured Streaming query is already running.");
            return;
        }

        log.info("Starting Real-time Spark Structured Streaming query on topic='{}', bootstrapServers='{}'", logsTopic, bootstrapServers);

        try {
            StructType logSchema = new StructType(new StructField[]{
                    new StructField("serviceName", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("logLevel", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("category", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("executionTimeMs", DataTypes.LongType, true, Metadata.empty()),
                    new StructField("timestamp", DataTypes.TimestampType, true, Metadata.empty())
            });

            Dataset<Row> rawStream = sparkSession.readStream()
                    .format("kafka")
                    .option("kafka.bootstrap.servers", bootstrapServers)
                    .option("subscribe", logsTopic)
                    .option("startingOffsets", "latest")
                    .option("failOnDataLoss", "false")
                    .load();

            Dataset<Row> parsedLogs = rawStream
                    .selectExpr("CAST(value AS STRING) as json_payload")
                    .select(functions.from_json(functions.col("json_payload"), logSchema).alias("data"))
                    .select("data.*")
                    .filter(functions.col("timestamp").isNotNull());

            Dataset<Row> windowedMetrics = parsedLogs
                    .withWatermark("timestamp", "10 seconds")
                    .groupBy(
                            functions.window(functions.col("timestamp"), "5 seconds"),
                            functions.col("logLevel")
                    )
                    .agg(
                            functions.count(functions.lit(1)).alias("count"),
                            functions.avg(functions.col("executionTimeMs")).alias("avg_execution_time")
                    );

            activeQuery = windowedMetrics.writeStream()
                    .outputMode(OutputMode.Update())
                    .trigger(Trigger.ProcessingTime("5 seconds"))
                    .foreachBatch((org.apache.spark.api.java.function.VoidFunction2<Dataset<Row>, Long>) (batchDf, batchId) -> processBatchAndPublishRedis(batchDf, batchId))
                    .start();

            log.info("Spark Structured Streaming query started successfully with queryId={}", activeQuery.id());
        } catch (Exception e) {
            log.error("Failed to start Spark Structured Streaming query: {}", e.getMessage(), e);
        }
    }

    private void processBatchAndPublishRedis(Dataset<Row> batchDf, long batchId) {
        if (batchDf.isEmpty()) {
            return;
        }

        try {
            List<Row> rows = batchDf.collectAsList();
            if (rows.isEmpty()) return;

            Map<String, Map<String, Object>> windowMap = new HashMap<>();

            for (Row row : rows) {
                Row windowStruct = row.getAs("window");
                if (windowStruct == null) continue;

                String startStr = windowStruct.getAs("start") != null ? windowStruct.getAs("start").toString() : "";
                String endStr = windowStruct.getAs("end") != null ? windowStruct.getAs("end").toString() : "";
                String windowKey = startStr + "_" + endStr;

                String level = row.getAs("logLevel") != null ? row.getAs("logLevel").toString().toUpperCase() : "INFO";
                long count = row.getLong(row.fieldIndex("count"));
                Number avgTimeNum = row.getAs("avg_execution_time");
                double avgTime = avgTimeNum != null ? avgTimeNum.doubleValue() : 0.0;

                Map<String, Object> windowData = windowMap.computeIfAbsent(windowKey, k -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("windowStart", startStr);
                    m.put("windowEnd", endStr);
                    m.put("totalLogs", 0L);
                    m.put("infoCount", 0L);
                    m.put("warnCount", 0L);
                    m.put("errorCount", 0L);
                    m.put("debugCount", 0L);
                    m.put("levelCounts", new HashMap<String, Long>());
                    m.put("avgExecutionTimeMs", avgTime);
                    return m;
                });

                long currentTotal = (long) windowData.get("totalLogs") + count;
                windowData.put("totalLogs", currentTotal);

                @SuppressWarnings("unchecked")
                Map<String, Long> levelCounts = (Map<String, Long>) windowData.get("levelCounts");
                levelCounts.put(level, count);

                if ("INFO".equalsIgnoreCase(level)) windowData.put("infoCount", count);
                else if ("WARN".equalsIgnoreCase(level)) windowData.put("warnCount", count);
                else if ("ERROR".equalsIgnoreCase(level)) windowData.put("errorCount", count);
                else if ("DEBUG".equalsIgnoreCase(level)) windowData.put("debugCount", count);
            }

            for (Map<String, Object> w : windowMap.values()) {
                long totalLogs = (long) w.get("totalLogs");
                double throughput = totalLogs / 5.0; // 5-second tumbling window

                @SuppressWarnings("unchecked")
                Map<String, Long> levelCounts = (Map<String, Long>) w.get("levelCounts");

                RealtimeLogMetricsDto dto = RealtimeLogMetricsDto.builder()
                        .windowStart((String) w.get("windowStart"))
                        .windowEnd((String) w.get("windowEnd"))
                        .throughputLogsPerSec(throughput)
                        .totalLogsCount(totalLogs)
                        .infoCount((Long) w.get("infoCount"))
                        .warnCount((Long) w.get("warnCount"))
                        .errorCount((Long) w.get("errorCount"))
                        .debugCount((Long) w.get("debugCount"))
                        .levelCounts(levelCounts)
                        .avgExecutionTimeMs((Double) w.get("avgExecutionTimeMs"))
                        .timestamp(Instant.now())
                        .build();

                String jsonMessage = objectMapper.writeValueAsString(dto);
                redisTemplate.convertAndSend(REDIS_REALTIME_CHANNEL, jsonMessage);
                log.debug("Published realtime window metric to Redis channel {}: totalLogs={}, throughput={}/s",
                        REDIS_REALTIME_CHANNEL, totalLogs, throughput);
            }
        } catch (Exception e) {
            log.error("Error processing streaming batch {} and publishing to Redis: {}", batchId, e.getMessage(), e);
        }
    }

    @Override
    public synchronized void stopLogStreamingQuery() {
        if (activeQuery != null) {
            try {
                log.info("Stopping Real-time Spark Structured Streaming query...");
                activeQuery.stop();
                activeQuery = null;
                log.info("Spark Structured Streaming query stopped.");
            } catch (Exception e) {
                log.error("Error stopping Spark Structured Streaming query: {}", e.getMessage(), e);
            }
        }
    }

    @Override
    public boolean isStreamingActive() {
        return activeQuery != null && activeQuery.isActive();
    }
}
