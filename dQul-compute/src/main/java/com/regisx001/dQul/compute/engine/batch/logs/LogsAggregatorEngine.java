package com.regisx001.dQul.compute.engine.batch.logs;

import com.regisx001.dQul.compute.dto.logs.LogsAggregationRequest;
import com.regisx001.dQul.compute.dto.logs.LogsAggregationResultDto;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

/**
 * Engine abstraction for Spark batch log aggregations.
 */
public interface LogsAggregatorEngine {

    /**
     * Executes batch log aggregation metrics calculation on a DataFrame of log entries.
     *
     * @param request the logs aggregation request
     * @param df the loaded logs DataFrame
     * @return populated LogsAggregationResultDto
     */
    LogsAggregationResultDto aggregate(LogsAggregationRequest request, Dataset<Row> df);
}
