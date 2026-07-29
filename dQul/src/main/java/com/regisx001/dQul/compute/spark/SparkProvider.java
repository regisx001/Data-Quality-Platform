package com.regisx001.dQul.compute.spark;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SparkProvider {

    private final SparkSession sparkSession;

    /**
     * Retrieves the configured and active SparkSession instance.
     *
     * @return active {@link SparkSession}
     */
    public SparkSession getSparkSession() {
        return sparkSession;
    }

    /**
     * Reads a dataset from a source format (e.g., csv, parquet, json, jdbc) with optional reader options.
     *
     * @param format  data source format (e.g. "csv", "parquet")
     * @param path    path or location of the resource
     * @param options reader options (key-value properties)
     * @return loaded {@link Dataset} of {@link Row}
     */
    public Dataset<Row> readDataset(String format, String path, Map<String, String> options) {
        var reader = sparkSession.read().format(format);
        if (options != null && !options.isEmpty()) {
            reader.options(options);
        }
        return reader.load(path);
    }

    /**
     * Executes a Spark SQL query string against the SparkSession context.
     *
     * @param sqlQuery SQL query string
     * @return resulting {@link Dataset} of {@link Row}
     */
    public Dataset<Row> executeSql(String sqlQuery) {
        log.debug("Executing Spark SQL query: {}", sqlQuery);
        return sparkSession.sql(sqlQuery);
    }

    /**
     * Checks if the underlying SparkSession is initialized and active.
     *
     * @return true if SparkSession is active and not stopped
     */
    public boolean isSparkActive() {
        return sparkSession != null && sparkSession.sparkContext() != null && !sparkSession.sparkContext().isStopped();
    }
}
