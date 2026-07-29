package com.regisx001.dQul.compute.spark;

import lombok.extern.slf4j.Slf4j;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class SparkProviderTest {

    private static SparkSession sparkSession;
    private static SparkProvider sparkProvider;

    @BeforeAll
    static void setUp() {
        log.info("==================================================");
        log.info("Starting Spark Integration Test");
        log.info("==================================================");

        SparkConf conf = new SparkConf()
                .setAppName("SparkProviderTestRunner")
                .setMaster("local[*]")
                .set("spark.ui.enabled", "false");

        sparkSession = SparkSession.builder()
                .config(conf)
                .getOrCreate();

        sparkProvider = new SparkProvider(sparkSession);

        log.info("SparkSession initialized.");
        log.info("Spark Master : {}", sparkSession.sparkContext().master());
        log.info("Spark App ID : {}", sparkSession.sparkContext().applicationId());
        log.info("Spark Active : {}", sparkProvider.isSparkActive());
    }

    @AfterAll
    static void tearDown() {
        if (sparkSession != null) {
            log.info("Stopping SparkSession...");
            sparkSession.stop();
            log.info("SparkSession stopped successfully.");
            log.info("==================================================");
        }
    }

    @Test
    @DisplayName("Verify Spark session status")
    void testSparkSessionIsActive() {
        assertTrue(sparkProvider.isSparkActive(), "Spark session should be active");
        assertNotNull(sparkProvider.getSparkSession(), "SparkSession instance should not be null");
    }

    @Test
    @DisplayName("Process sample text file and perform word count")
    void testWordCountProcessing(@TempDir Path tempDir) throws IOException {
        // 1. Create a sample text file
        Path sampleFile = tempDir.resolve("sample_words.txt");
        List<String> lines = List.of(
                "Spark is fast and general purpose processing engine",
                "Spark SQL is a Spark module for structured data processing",
                "Data Quality Platform powered by Spark"
        );
        Files.write(sampleFile, lines);
        log.info("Created sample test file at: {}", sampleFile.toAbsolutePath());

        // 2. Load dataset via SparkProvider
        Dataset<Row> rawData = sparkProvider.readDataset("text", sampleFile.toString(), null);
        long lineCount = rawData.count();
        log.info("Loaded text file into Spark. Total lines: {}", lineCount);
        assertEquals(3, lineCount, "Expected 3 lines in test file");

        // 3. Process data using Spark DataFrames (split words, lowercase, group and count)
        Dataset<Row> wordCounts = rawData
                .select(functions.explode(functions.split(functions.col("value"), "\\s+")).as("word"))
                .select(functions.lower(functions.col("word")).as("word"))
                .groupBy("word")
                .count()
                .orderBy(functions.col("count").desc());

        log.info("--- Word Count Execution Results ---");
        List<Row> results = wordCounts.collectAsList();
        for (Row row : results) {
            log.info("Word: {:<15} | Count: {}", row.getString(0), row.getLong(1));
        }

        // 4. Assert processing results
        assertNotNull(results);
        assertFalse(results.isEmpty());

        // Validate count for word "spark" (appears 4 times in test lines)
        Row sparkResult = wordCounts.filter(functions.col("word").equalTo("spark")).first();
        log.info("Verified 'spark' word count: {}", sparkResult.getLong(1));
        assertEquals(4L, sparkResult.getLong(1), "Word 'spark' count should be 4");
    }
}
