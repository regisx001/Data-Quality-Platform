package com.regisx001.dQul.compute.io.storage.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.regisx001.dQul.compute.dto.logs.LogsAggregationResultDto;
import com.regisx001.dQul.compute.io.storage.LogsStorageService;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class S3LogsStorageServiceImpl implements LogsStorageService {

    private static final Logger log = LoggerFactory.getLogger(S3LogsStorageServiceImpl.class);

    private final SparkSession sparkSession;
    private final ObjectMapper objectMapper;
    private final String defaultBucket;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public S3LogsStorageServiceImpl(SparkSession sparkSession,
                                    @Value("${spark.s3.results-bucket:dqul-results}") String defaultBucket) {
        this.sparkSession = sparkSession;
        this.defaultBucket = defaultBucket;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String saveLogsAggregationResult(LogsAggregationResultDto resultDto) {
        UUID uuid = resultDto.getJobId() != null ? resultDto.getJobId() : UUID.randomUUID();
        String timestampStr = LocalDateTime.now().format(TIMESTAMP_FORMATTER);

        // Filename composed of UUID and timestamp
        String filename = String.format("%s_%s.json", uuid, timestampStr);
        String s3Uri = String.format("s3a://%s/logs-aggregations/%s", defaultBucket, filename);

        log.info("Persisting batch logs aggregation result to S3 URI: {}", s3Uri);

        try {
            byte[] jsonBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(resultDto);

            Configuration hadoopConf = sparkSession.sparkContext().hadoopConfiguration();
            Path path = new Path(s3Uri);
            FileSystem fs = FileSystem.get(URI.create(s3Uri), hadoopConf);

            if (!fs.exists(path.getParent())) {
                fs.mkdirs(path.getParent());
            }

            try (FSDataOutputStream out = fs.create(path, true)) {
                out.write(jsonBytes);
                out.flush();
            }

            log.info("Successfully wrote {} bytes to logs aggregation result file: {}", jsonBytes.length, s3Uri);
            return s3Uri;
        } catch (Exception e) {
            log.error("Failed to save logs aggregation result to S3 URI {}: {}", s3Uri, e.getMessage(), e);
            throw new RuntimeException("Error saving logs aggregation result to S3: " + e.getMessage(), e);
        }
    }
}
