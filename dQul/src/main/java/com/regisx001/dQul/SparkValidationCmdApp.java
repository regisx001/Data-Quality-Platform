package com.regisx001.dQul;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.stereotype.Component;

import com.regisx001.dQul.compute.spark.SparkProvider;

/**
 * Command-line runner testing SparkSession via SparkProvider.
 * 
 * Dynamically selects a dataset from uploads/csv/. If no CSV dataset exists in
 * uploads/csv/, it runs in Spark test mode to verify Spark operations.
 */
// @Component
public class SparkValidationCmdApp implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SparkValidationCmdApp.class);

    private static final String UPLOADS_CSV_DIR = "uploads/csv";

    private final SparkProvider sparkProvider;

    public SparkValidationCmdApp(SparkProvider sparkProvider) {
        this.sparkProvider = sparkProvider;
    }

    public static void main(String[] args) {
        log.info("Starting Spark Validation Command Line Application...");
        new SpringApplicationBuilder(SparkValidationCmdApp.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("==========================================================================");
        System.out.println("            SPARK SESSION DATASET VALIDATION COMMAND LINE APP             ");
        System.out.println("==========================================================================");

        // 1. Initialize & Test SparkSession using SparkProvider
        System.out.println("\n[1/4] Testing SparkSession via SparkProvider...");
        if (!sparkProvider.isSparkActive()) {
            System.err.println("[ERROR] SparkProvider reports that SparkSession is NOT active!");
            return;
        }

        SparkSession spark = sparkProvider.getSparkSession();
        System.out.println(" -> SparkProvider active status : " + sparkProvider.isSparkActive());
        System.out.println(" -> Spark App Name             : " + spark.sparkContext().appName());
        System.out.println(" -> Spark Version              : " + spark.version());
        System.out.println(" -> Spark Master               : " + spark.sparkContext().master());

        // 2. Discover CSV Dataset in uploads/csv directory
        System.out.println("\n[2/4] Searching for dataset in 'uploads/csv/' directory...");
        Path targetPath = discoverCsvDataset(args);

        if (targetPath == null) {
            System.out.println("[WARNING] No CSV dataset found in '" + UPLOADS_CSV_DIR + "'.");
            System.out.println(" -> Executing Spark test mode to verify SparkSession functionality...");

            Dataset<Row> testDf = sparkProvider.executeSql(
                    "SELECT 'Spark Engine Operational' AS status, current_timestamp() AS timestamp, 42 AS test_value");
            testDf.show(false);

            System.out.println("[SUCCESS] SparkSession test mode completed successfully!");
            System.out.println("==========================================================================\n");
            return;
        }

        System.out.println(" -> Selected CSV dataset file : " + targetPath.toAbsolutePath());
        System.out.println(" -> File size                 : " + Files.size(targetPath) + " bytes");

        try {
            // 3. Load Dataset using SparkProvider.readDataset()
            System.out.println("\n[3/4] Reading dataset using SparkProvider.readDataset()...");
            Map<String, String> csvOptions = Map.of(
                    "header", "true",
                    "inferSchema", "true",
                    "emptyValue", "",
                    "nullValue", "");

            Dataset<Row> df = sparkProvider.readDataset("csv", targetPath.toAbsolutePath().toString(), csvOptions);

            long totalRows = df.count();
            String[] columns = df.columns();
            System.out.println(" -> Dataset successfully loaded.");
            System.out.println(" -> Total Rows   : " + totalRows);
            System.out.println(" -> Total Columns: " + columns.length);
            System.out.println(" -> Schema:");
            df.printSchema();

            // 4. Validate Missing / Null Values
            System.out.println("\n[4/4] Validating dataset for missing/null values...");

            long totalMissingValues = 0;
            Map<String, Long> columnMissingCounts = new HashMap<>();

            for (String colName : columns) {
                Column isMissingCond = functions.col(colName).isNull()
                        .or(functions.isnan(functions.col(colName)))
                        .or(functions.trim(functions.col(colName)).equalTo(""));

                long missingCount = df.filter(isMissingCond).count();
                columnMissingCounts.put(colName, missingCount);
                totalMissingValues += missingCount;
            }

            // Report Results
            System.out.println("\n==========================================================================");
            System.out.println("                         VALIDATION RESULTS                               ");
            System.out.println("==========================================================================");
            System.out.printf("%-30s | %-15s | %-15s | %-15s\n", "Column Name", "Data Type", "Missing Count",
                    "Missing %");
            System.out.println("--------------------------------------------------------------------------");

            org.apache.spark.sql.types.StructType schema = df.schema();
            for (String colName : columns) {
                long missingCount = columnMissingCounts.get(colName);
                double missingPct = totalRows > 0 ? ((double) missingCount / totalRows) * 100.0 : 0.0;
                String dataType = schema.apply(colName).dataType().typeName();
                System.out.printf("%-30s | %-15s | %-15d | %-14.2f%%\n", colName, dataType, missingCount, missingPct);
            }

            System.out.println("--------------------------------------------------------------------------");
            System.out.println("Total Missing / Null Values across dataset: " + totalMissingValues);

            if (totalMissingValues == 0) {
                System.out.println("[VALIDATION SUCCESS] The dataset HAS NO MISSING VALUES!");
            } else {
                System.out.println(
                        "[VALIDATION WARNING] The dataset CONTAINS " + totalMissingValues + " MISSING VALUES.");
            }
            System.out.println("==========================================================================\n");

        } catch (Exception e) {
            System.err.println("[ERROR] Error during dataset validation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Path discoverCsvDataset(String... args) {
        if (args != null && args.length > 0 && !args[0].isBlank()) {
            Path cliPath = Paths.get(args[0]).toAbsolutePath().normalize();
            if (Files.exists(cliPath) && Files.isRegularFile(cliPath)) {
                return cliPath;
            }
        }

        Path[] candidateDirs = new Path[] {
                Paths.get(UPLOADS_CSV_DIR),
                Paths.get("dQul", UPLOADS_CSV_DIR),
                Paths.get("..", UPLOADS_CSV_DIR),
                Paths.get(System.getProperty("user.dir"), UPLOADS_CSV_DIR),
                Paths.get(System.getProperty("user.dir"), "dQul", UPLOADS_CSV_DIR)
        };

        for (Path dir : candidateDirs) {
            Path normalizedDir = dir.toAbsolutePath().normalize();
            if (Files.exists(normalizedDir) && Files.isDirectory(normalizedDir)) {
                try (Stream<Path> stream = Files.list(normalizedDir)) {
                    Optional<Path> csvFile = stream
                            .filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".csv"))
                            .findFirst();
                    if (csvFile.isPresent()) {
                        return csvFile.get();
                    }
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }
}
