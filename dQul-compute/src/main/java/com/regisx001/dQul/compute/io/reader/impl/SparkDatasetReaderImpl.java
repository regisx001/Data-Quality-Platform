package com.regisx001.dQul.compute.io.reader.impl;

import com.regisx001.dQul.compute.dto.DatasetProfileRequest;
import com.regisx001.dQul.compute.io.reader.DatasetReader;
import org.apache.spark.sql.DataFrameReader;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SparkDatasetReaderImpl implements DatasetReader {

    private static final Logger log = LoggerFactory.getLogger(SparkDatasetReaderImpl.class);

    private final SparkSession sparkSession;

    public SparkDatasetReaderImpl(SparkSession sparkSession) {
        this.sparkSession = sparkSession;
    }

    @Override
    public Dataset<Row> read(DatasetProfileRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("DatasetProfileRequest must not be null");
        }

        String sourceType = request.getSourceType() != null ? request.getSourceType().toUpperCase() : "S3_CSV";

        if ("POSTGRES_JDBC".equals(sourceType) || "JDBC".equals(sourceType)) {
            return readJdbc(request);
        } else {
            return readCsv(request);
        }
    }

    private Dataset<Row> readJdbc(DatasetProfileRequest request) {
        DatasetProfileRequest.JdbcConfig jdbc = request.getJdbcConfig();
        if (jdbc == null || jdbc.getUrl() == null || jdbc.getDbtable() == null) {
            throw new IllegalArgumentException("JdbcConfig with url and dbtable is required for POSTGRES_JDBC profiling");
        }
        log.info("Reading JDBC dataset table '{}' from '{}'", jdbc.getDbtable(), jdbc.getUrl());

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
    }

    private Dataset<Row> readCsv(DatasetProfileRequest request) {
        String s3aUri = request.getS3aUri();
        if (s3aUri == null || s3aUri.isBlank()) {
            throw new IllegalArgumentException("s3aUri is required for S3_CSV profiling");
        }
        log.info("Reading CSV dataset from S3/File URI: {}", s3aUri);

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
