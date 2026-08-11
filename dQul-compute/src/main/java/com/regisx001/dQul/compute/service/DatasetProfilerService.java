package com.regisx001.dQul.compute.service;

import com.regisx001.dQul.compute.dto.ColumnProfileDto;
import com.regisx001.dQul.compute.dto.DatasetProfileRequest;
import com.regisx001.dQul.compute.dto.TableProfileDto;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.DataFrameReader;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.NumericType;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatasetProfilerService {

    private static final Logger log = LoggerFactory.getLogger(DatasetProfilerService.class);

    private final SparkSession sparkSession;

    public DatasetProfilerService(SparkSession sparkSession) {
        this.sparkSession = sparkSession;
    }

    public TableProfileDto profile(DatasetProfileRequest request) {
        log.info("Starting dataset profiling for profileId={}, datasetId={}, sourceType={}",
                request.getProfileId(), request.getDatasetId(), request.getSourceType());

        Dataset<Row> df = loadDataset(request);
        long totalRows = df.count();
        StructType schema = df.schema();
        String[] columnNames = df.columns();

        log.info("Loaded dataset with totalRows={}, columnCount={}", totalRows, columnNames.length);

        List<ColumnProfileDto> columnProfiles = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        if (columnNames.length > 0 && totalRows > 0) {
            // Build single Spark aggregation query for performance
            List<Column> aggExprs = new ArrayList<>();
            for (String colName : columnNames) {
                Column col = functions.col(colName);
                StructField field = schema.apply(colName);
                DataType dataType = field.dataType();

                // Null count condition (isnan is valid only for FloatType or DoubleType)
                boolean isFloatingPoint = (dataType instanceof org.apache.spark.sql.types.DoubleType) || (dataType instanceof org.apache.spark.sql.types.FloatType);
                Column isNullCond = isFloatingPoint ? col.isNull().or(functions.isnan(col)) : col.isNull();

                aggExprs.add(functions.sum(functions.when(isNullCond, 1L).otherwise(0L)).alias(colName + "_nulls"));
                // Distinct count
                aggExprs.add(functions.countDistinct(col).alias(colName + "_distinct"));
                // Statistical Summary (min, max, avg) is computed ONLY for numerical and date/timestamp values
                boolean isNumericOrDate = (dataType instanceof NumericType)
                        || (dataType instanceof org.apache.spark.sql.types.DateType)
                        || (dataType instanceof org.apache.spark.sql.types.TimestampType)
                        || (dataType instanceof org.apache.spark.sql.types.TimestampNTZType);

                if (isNumericOrDate) {
                    // Min & Max value
                    aggExprs.add(functions.min(col).cast("string").alias(colName + "_min"));
                    aggExprs.add(functions.max(col).cast("string").alias(colName + "_max"));

                    // Avg value for numeric types
                    if (dataType instanceof NumericType) {
                        aggExprs.add(functions.avg(col).alias(colName + "_avg"));
                    }
                }
            }

            Row aggResult = df.select(aggExprs.toArray(new Column[0])).first();

            for (String colName : columnNames) {
                StructField field = schema.apply(colName);
                DataType dataType = field.dataType();

                Long nullCount = aggResult.getAs(colName + "_nulls");
                if (nullCount == null) nullCount = 0L;

                double nullPct = totalRows > 0 ? ((double) nullCount / totalRows) * 100.0 : 0.0;

                Long distinctCount = aggResult.getAs(colName + "_distinct");
                if (distinctCount == null) distinctCount = 0L;

                boolean isNumericOrDate = (dataType instanceof NumericType)
                        || (dataType instanceof org.apache.spark.sql.types.DateType)
                        || (dataType instanceof org.apache.spark.sql.types.TimestampType)
                        || (dataType instanceof org.apache.spark.sql.types.TimestampNTZType);

                String minVal = null;
                String maxVal = null;
                Double avgVal = null;

                if (isNumericOrDate) {
                    minVal = aggResult.getAs(colName + "_min");
                    maxVal = aggResult.getAs(colName + "_max");

                    if (dataType instanceof NumericType) {
                        Number avgNum = aggResult.getAs(colName + "_avg");
                        if (avgNum != null) {
                            avgVal = avgNum.doubleValue();
                        }
                    }
                }

                columnProfiles.add(ColumnProfileDto.builder()
                        .columnName(colName)
                        .dataType(dataType.typeName())
                        .nullCount(nullCount)
                        .nullPercentage(nullPct)
                        .distinctCount(distinctCount)
                        .minValue(minVal)
                        .maxValue(maxVal)
                        .avgValue(avgVal)
                        .profiledAt(now)
                        .build());
            }
        } else if (columnNames.length > 0) {
            // Empty dataset with columns
            for (String colName : columnNames) {
                StructField field = schema.apply(colName);
                columnProfiles.add(ColumnProfileDto.builder()
                        .columnName(colName)
                        .dataType(field.dataType().typeName())
                        .nullCount(0L)
                        .nullPercentage(0.0)
                        .distinctCount(0L)
                        .profiledAt(now)
                        .build());
            }
        }

        String tableName = extractTableName(request);

        return TableProfileDto.builder()
                .profileId(request.getProfileId())
                .datasetId(request.getDatasetId())
                .tableName(tableName)
                .rowCount(totalRows)
                .columnCount(columnNames.length)
                .columnProfiles(columnProfiles)
                .profiledAt(now)
                .build();
    }

    private Dataset<Row> loadDataset(DatasetProfileRequest request) {
        String sourceType = request.getSourceType() != null ? request.getSourceType().toUpperCase() : "S3_CSV";

        if ("POSTGRES_JDBC".equals(sourceType) || "JDBC".equals(sourceType)) {
            DatasetProfileRequest.JdbcConfig jdbc = request.getJdbcConfig();
            if (jdbc == null || jdbc.getUrl() == null || jdbc.getDbtable() == null) {
                throw new IllegalArgumentException("JdbcConfig with url and dbtable is required for POSTGRES_JDBC profiling");
            }
            log.info("Reading JDBC table '{}' from '{}'", jdbc.getDbtable(), jdbc.getUrl());
            DataFrameReader reader = sparkSession.read()
                    .format("jdbc")
                    .option("url", jdbc.getUrl())
                    .option("dbtable", jdbc.getDbtable());

            if (jdbc.getUser() != null) reader.option("user", jdbc.getUser());
            if (jdbc.getPassword() != null) reader.option("password", jdbc.getPassword());
            if (jdbc.getDriver() != null) {
                reader.option("driver", jdbc.getDriver());
            } else {
                reader.option("driver", "org.postgresql.Driver");
            }

            return reader.load();
        } else {
            // Default S3_CSV
            String s3aUri = request.getS3aUri();
            if (s3aUri == null || s3aUri.isBlank()) {
                throw new IllegalArgumentException("s3aUri is required for S3_CSV profiling");
            }
            log.info("Reading CSV dataset from S3 URI: {}", s3aUri);
            DataFrameReader reader = sparkSession.read().format("csv");

            Map<String, String> defaultOptions = new HashMap<>(Map.of(
                    "header", "true",
                    "inferSchema", "true"
            ));

            if (request.getCsvOptions() != null) {
                defaultOptions.putAll(request.getCsvOptions());
            }

            return reader.options(defaultOptions).load(s3aUri);
        }
    }

    private String extractTableName(DatasetProfileRequest request) {
        if (request.getJdbcConfig() != null && request.getJdbcConfig().getDbtable() != null) {
            return request.getJdbcConfig().getDbtable();
        }
        if (request.getS3aUri() != null) {
            String path = request.getS3aUri();
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash != -1 && lastSlash < path.length() - 1) {
                String filename = path.substring(lastSlash + 1);
                int dotIndex = filename.lastIndexOf('.');
                return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
            }
        }
        return "dataset_" + (request.getDatasetId() != null ? request.getDatasetId().toString() : "unknown");
    }
}
