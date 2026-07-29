package com.regisx001.dQul.connector.api;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

/**
 * Abstraction that allows the Spark compute engine to read a dataset
 * from a connector-backed datasource without knowing its physical location
 * or storage technology.
 */
@FunctionalInterface
public interface DataReader {

    /**
     * Reads the dataset into a Spark {@link Dataset<Row>}.
     *
     * @return a Spark DataFrame containing the dataset's data
     */
    Dataset<Row> read();
}
