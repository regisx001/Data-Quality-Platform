package com.regisx001.dQul.compute.io.reader;

import com.regisx001.dQul.compute.dto.DatasetProfileRequest;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

/**
 * Abstraction for reading datasets (CSV/S3, Postgres JDBC, Parquet, Delta, etc.)
 * into Spark DataFrames.
 */
public interface DatasetReader {

    /**
     * Loads a dataset as a Spark DataFrame based on request properties.
     *
     * @param request the dataset profile request
     * @return loaded Spark Dataset<Row>
     */
    Dataset<Row> read(DatasetProfileRequest request);
}
