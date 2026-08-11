package com.regisx001.dQul.compute.service;

import com.regisx001.dQul.compute.dto.ColumnProfileDto;
import com.regisx001.dQul.compute.dto.DatasetProfileRequest;
import com.regisx001.dQul.compute.dto.TableProfileDto;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DatasetProfilerServiceTest {

    private static SparkSession spark;

    @BeforeAll
    static void setupSpark() {
        spark = SparkSession.builder()
                .master("local[*]")
                .appName("DatasetProfilerServiceTest")
                .config("spark.ui.enabled", "false")
                .getOrCreate();
    }

    @AfterAll
    static void tearDownSpark() {
        if (spark != null) {
            spark.stop();
        }
    }

    @Test
    void testProfileLocalCsvDataset(@TempDir Path tempDir) throws Exception {
        File csvFile = tempDir.resolve("test_sales.csv").toFile();
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("id,age,score,city\n");
            writer.write("1,25,85.5,New York\n");
            writer.write("2,30,92.0,Boston\n");
            writer.write("3,,78.0,Boston\n");
            writer.write("4,40,,Chicago\n");
        }

        DatasetProfilerService profilerService = new DatasetProfilerService(spark);

        UUID profileId = UUID.randomUUID();
        UUID datasetId = UUID.randomUUID();

        DatasetProfileRequest request = DatasetProfileRequest.builder()
                .profileId(profileId)
                .datasetId(datasetId)
                .sourceType("S3_CSV")
                .s3aUri(csvFile.getAbsolutePath())
                .csvOptions(Map.of("header", "true", "inferSchema", "true"))
                .build();

        TableProfileDto result = profilerService.profile(request);

        assertNotNull(result);
        assertEquals(profileId, result.getProfileId());
        assertEquals(datasetId, result.getDatasetId());
        assertEquals(4L, result.getRowCount());
        assertEquals(4, result.getColumnCount());
        assertEquals(4, result.getColumnProfiles().size());

        // Check age column metrics (has 1 null out of 4)
        ColumnProfileDto ageProfile = result.getColumnProfiles().stream()
                .filter(c -> "age".equals(c.getColumnName()))
                .findFirst()
                .orElseThrow();

        assertEquals(1L, ageProfile.getNullCount());
        assertEquals(25.0, ageProfile.getNullPercentage(), 0.01);
        assertEquals(3L, ageProfile.getDistinctCount());
        assertNotNull(ageProfile.getAvgValue());

        // Check city column metrics
        ColumnProfileDto cityProfile = result.getColumnProfiles().stream()
                .filter(c -> "city".equals(c.getColumnName()))
                .findFirst()
                .orElseThrow();

        assertEquals(0L, cityProfile.getNullCount());
        assertEquals(3L, cityProfile.getDistinctCount()); // New York, Boston, Chicago
    }
}
