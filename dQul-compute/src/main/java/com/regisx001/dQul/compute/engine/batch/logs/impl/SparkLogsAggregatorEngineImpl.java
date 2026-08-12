package com.regisx001.dQul.compute.engine.batch.logs.impl;

import com.regisx001.dQul.compute.dto.logs.LogsAggregationRequest;
import com.regisx001.dQul.compute.dto.logs.LogsAggregationResultDto;
import com.regisx001.dQul.compute.engine.batch.logs.LogsAggregatorEngine;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.functions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SparkLogsAggregatorEngineImpl implements LogsAggregatorEngine {

    private static final Logger log = LoggerFactory.getLogger(SparkLogsAggregatorEngineImpl.class);

    @Override
    public LogsAggregationResultDto aggregate(LogsAggregationRequest request, Dataset<Row> df) {
        log.info("Executing Spark batch logs aggregation for jobId={}", request.getJobId());

        if (df == null) {
            throw new IllegalArgumentException("DataFrame must not be null");
        }

        List<String> colList = Arrays.asList(df.columns());

        // Resolve column names (supporting both snake_case from DB and camelCase from JSON/CSV)
        String levelCol = colList.contains("log_level") ? "log_level" : (colList.contains("logLevel") ? "logLevel" : null);
        String serviceCol = colList.contains("service_name") ? "service_name" : (colList.contains("serviceName") ? "serviceName" : null);
        String categoryCol = colList.contains("category") ? "category" : null;
        String execTimeCol = colList.contains("execution_time_ms") ? "execution_time_ms" : (colList.contains("executionTimeMs") ? "executionTimeMs" : null);
        String tsCol = colList.contains("timestamp") ? "timestamp" : null;
        String msgCol = colList.contains("message") ? "message" : null;

        long totalCount = df.count();
        log.info("Loaded {} log entries for aggregation", totalCount);

        Map<String, Long> levelCounts = extractGroupCounts(df, levelCol);
        Map<String, Long> serviceCounts = extractGroupCounts(df, serviceCol);
        Map<String, Long> categoryCounts = extractGroupCounts(df, categoryCol);

        Double avgExecutionTime = null;
        Long maxExecutionTime = null;

        if (execTimeCol != null && totalCount > 0) {
            Row statsRow = df.select(
                    functions.avg(functions.col(execTimeCol)).alias("avg_exec"),
                    functions.max(functions.col(execTimeCol)).alias("max_exec")
            ).first();

            if (statsRow != null) {
                Number avgNum = statsRow.getAs("avg_exec");
                if (avgNum != null) avgExecutionTime = avgNum.doubleValue();

                Number maxNum = statsRow.getAs("max_exec");
                if (maxNum != null) maxExecutionTime = maxNum.longValue();
            }
        }

        String minTimestamp = null;
        String maxTimestamp = null;

        if (tsCol != null && totalCount > 0) {
            Row tsRow = df.select(
                    functions.min(functions.col(tsCol)).cast("string").alias("min_ts"),
                    functions.max(functions.col(tsCol)).cast("string").alias("max_ts")
            ).first();

            if (tsRow != null) {
                minTimestamp = tsRow.getAs("min_ts");
                maxTimestamp = tsRow.getAs("max_ts");
            }
        }

        List<LogsAggregationResultDto.ErrorLogSummaryDto> topErrorMessages = extractTopErrors(df, levelCol, serviceCol, categoryCol, msgCol);

        return LogsAggregationResultDto.builder()
                .jobId(request.getJobId())
                .totalLogsCount(totalCount)
                .levelCounts(levelCounts)
                .serviceCounts(serviceCounts)
                .categoryCounts(categoryCounts)
                .avgExecutionTimeMs(avgExecutionTime)
                .maxExecutionTimeMs(maxExecutionTime)
                .topErrorMessages(topErrorMessages)
                .minTimestamp(minTimestamp)
                .maxTimestamp(maxTimestamp)
                .aggregatedAt(LocalDateTime.now())
                .build();
    }

    private Map<String, Long> extractGroupCounts(Dataset<Row> df, String colName) {
        Map<String, Long> resultMap = new HashMap<>();
        if (colName == null) return resultMap;

        try {
            List<Row> rows = df.groupBy(colName)
                    .count()
                    .orderBy(functions.col("count").desc())
                    .collectAsList();

            for (Row row : rows) {
                Object keyObj = row.get(0);
                String key = keyObj != null ? keyObj.toString() : "UNKNOWN";
                long count = row.getLong(1);
                resultMap.put(key, count);
            }
        } catch (Exception e) {
            log.warn("Failed to extract group counts for column '{}': {}", colName, e.getMessage());
        }
        return resultMap;
    }

    private List<LogsAggregationResultDto.ErrorLogSummaryDto> extractTopErrors(Dataset<Row> df,
                                                                               String levelCol,
                                                                               String serviceCol,
                                                                               String categoryCol,
                                                                               String msgCol) {
        List<LogsAggregationResultDto.ErrorLogSummaryDto> errorList = new ArrayList<>();
        if (levelCol == null || msgCol == null) return errorList;

        try {
            Dataset<Row> errorDf = df.filter(functions.upper(functions.col(levelCol)).equalTo("ERROR"));

            List<Column> groupCols = new ArrayList<>();
            if (serviceCol != null) groupCols.add(functions.col(serviceCol));
            if (categoryCol != null) groupCols.add(functions.col(categoryCol));
            groupCols.add(functions.col(msgCol));

            List<Row> topErrors = errorDf.groupBy(groupCols.toArray(new Column[0]))
                    .count()
                    .orderBy(functions.col("count").desc())
                    .limit(10)
                    .collectAsList();

            for (Row row : topErrors) {
                String service = serviceCol != null ? (row.get(0) != null ? row.get(0).toString() : "UNKNOWN") : "N/A";
                int msgIndex = serviceCol != null ? (categoryCol != null ? 2 : 1) : (categoryCol != null ? 1 : 0);
                int categoryIndex = serviceCol != null ? 1 : 0;

                String category = categoryCol != null ? (row.get(categoryIndex) != null ? row.get(categoryIndex).toString() : "GENERAL") : "GENERAL";
                String msg = row.get(msgIndex) != null ? row.get(msgIndex).toString() : "";
                long count = row.getLong(row.length() - 1);

                errorList.add(LogsAggregationResultDto.ErrorLogSummaryDto.builder()
                        .serviceName(service)
                        .category(category)
                        .message(msg)
                        .count(count)
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to extract top errors: {}", e.getMessage());
        }

        return errorList;
    }
}
