package com.regisx001.dQul.compute.engine.batch.profiler.impl;

import com.regisx001.dQul.compute.dto.ColumnProfileDto;
import com.regisx001.dQul.compute.dto.DatasetProfileRequest;
import com.regisx001.dQul.compute.dto.TableProfileDto;
import com.regisx001.dQul.compute.engine.batch.profiler.DatasetProfilerEngine;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.NumericType;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DatasetProfilerEngineImpl implements DatasetProfilerEngine {

    private static final Logger log = LoggerFactory.getLogger(DatasetProfilerEngineImpl.class);

    @Override
    public TableProfileDto profile(DatasetProfileRequest request, Dataset<Row> df) {
        log.info("Executing Spark batch dataset profiling for profileId={}, datasetId={}",
                request.getProfileId(), request.getDatasetId());

        long totalRows = df.count();
        StructType schema = df.schema();
        String[] columnNames = df.columns();

        log.info("Loaded DataFrame with totalRows={}, columnCount={}", totalRows, columnNames.length);

        List<ColumnProfileDto> columnProfiles = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        if (columnNames.length > 0 && totalRows > 0) {
            List<Column> aggExprs = new ArrayList<>();
            for (String colName : columnNames) {
                Column col = functions.col(colName);
                StructField field = schema.apply(colName);
                DataType dataType = field.dataType();

                // Null count condition (isnan is valid only for FloatType or DoubleType)
                boolean isFloatingPoint = (dataType instanceof org.apache.spark.sql.types.DoubleType)
                        || (dataType instanceof org.apache.spark.sql.types.FloatType);
                Column isNullCond = isFloatingPoint ? col.isNull().or(functions.isnan(col)) : col.isNull();

                aggExprs.add(functions.sum(functions.when(isNullCond, 1L).otherwise(0L)).alias(colName + "_nulls"));
                aggExprs.add(functions.countDistinct(col).alias(colName + "_distinct"));

                // Statistical Summary (min, max, avg) is computed ONLY for numerical and date/timestamp values
                boolean isNumericOrDate = (dataType instanceof NumericType)
                        || (dataType instanceof org.apache.spark.sql.types.DateType)
                        || (dataType instanceof org.apache.spark.sql.types.TimestampType)
                        || (dataType instanceof org.apache.spark.sql.types.TimestampNTZType);

                if (isNumericOrDate) {
                    aggExprs.add(functions.min(col).cast("string").alias(colName + "_min"));
                    aggExprs.add(functions.max(col).cast("string").alias(colName + "_max"));

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
