package com.regisx001.dQul.compute.engine.batch.profiler;

import com.regisx001.dQul.compute.dto.DatasetProfileRequest;
import com.regisx001.dQul.compute.dto.TableProfileDto;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

/**
 * Abstraction for batch dataset profiling calculation engines.
 */
public interface DatasetProfilerEngine {

    /**
     * Profiles a Spark DataFrame and computes statistical summaries.
     *
     * @param request the dataset profile request
     * @param df the loaded Spark DataFrame
     * @return populated TableProfileDto with table & column metrics
     */
    TableProfileDto profile(DatasetProfileRequest request, Dataset<Row> df);
}
