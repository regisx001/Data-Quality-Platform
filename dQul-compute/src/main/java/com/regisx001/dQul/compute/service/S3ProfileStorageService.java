package com.regisx001.dQul.compute.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.regisx001.dQul.compute.dto.TableProfileDto;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class S3ProfileStorageService {

    private static final Logger log = LoggerFactory.getLogger(S3ProfileStorageService.class);

    private final SparkSession sparkSession;
    private final ObjectMapper objectMapper;
    private final String defaultBucket;

    public S3ProfileStorageService(SparkSession sparkSession,
            @Value("${spark.s3.results-bucket:dqul-bucket}") String defaultBucket) {
        this.sparkSession = sparkSession;
        this.defaultBucket = defaultBucket;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public String saveProfileResult(TableProfileDto profileDto) {
        UUID profileId = profileDto.getProfileId();
        String s3Uri = String.format("s3a://%s/profiles/%s.json", defaultBucket, profileId);

        log.info("Saving single profile JSON document to S3 URI: {}", s3Uri);

        try {
            byte[] jsonBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(profileDto);

            Configuration hadoopConf = sparkSession.sparkContext().hadoopConfiguration();
            Path path = new Path(s3Uri);
            FileSystem fs = FileSystem.get(URI.create(s3Uri), hadoopConf);

            // Ensure parent directory exists
            if (!fs.exists(path.getParent())) {
                fs.mkdirs(path.getParent());
            }

            try (FSDataOutputStream out = fs.create(path, true)) {
                out.write(jsonBytes);
                out.flush();
            }

            log.info("Successfully wrote {} bytes to single JSON file {}", jsonBytes.length, s3Uri);
            return s3Uri;
        } catch (Exception e) {
            log.error("Failed to save profile result to S3 URI {}: {}", s3Uri, e.getMessage(), e);
            throw new RuntimeException("Error saving profile result to S3: " + e.getMessage(), e);
        }
    }
}
