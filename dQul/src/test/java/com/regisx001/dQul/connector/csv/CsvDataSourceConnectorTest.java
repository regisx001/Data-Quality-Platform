package com.regisx001.dQul.connector.csv;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.regisx001.dQul.compute.spark.SparkSessionProvider;
import com.regisx001.dQul.connector.ConnectorConfig;
import com.regisx001.dQul.connector.api.ConnectionTestResult;
import com.regisx001.dQul.connector.api.DataType;
import com.regisx001.dQul.connector.api.DatasetType;

class CsvDataSourceConnectorTest {

    private static SparkSession sparkSession;
    private static SparkSessionProvider sparkSessionProvider;

    @BeforeAll
    static void setUp() {
        SparkConf conf = new SparkConf()
                .setAppName("CsvConnectorTest")
                .setMaster("local[1]")
                .set("spark.ui.enabled", "false");
        sparkSession = SparkSession.builder().config(conf).getOrCreate();
        sparkSessionProvider = new SparkSessionProvider(sparkSession);
    }

    @AfterAll
    static void tearDown() {
        if (sparkSession != null) sparkSession.stop();
    }

    @Test
    @DisplayName("testConnection returns failure when file does not exist")
    void testConnection_fileNotFound() {
        var config = new ConnectorConfig.Csv("/nonexistent/file.csv", "missing");
        var connector = new CsvDataSourceConnector(config, sparkSessionProvider);

        ConnectionTestResult result = connector.testConnection();

        assertFalse(result.success());
        assertTrue(result.message().contains("not found"));
    }

    @Test
    @DisplayName("testConnection returns success for an existing readable file")
    void testConnection_success(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("test.csv");
        Files.writeString(csvFile, "a,b\n1,2\n", StandardCharsets.UTF_8);

        var config = new ConnectorConfig.Csv(csvFile.toString(), "test");
        var connector = new CsvDataSourceConnector(config, sparkSessionProvider);

        ConnectionTestResult result = connector.testConnection();

        assertTrue(result.success());
        assertTrue(result.message().contains("verified"));
    }

    @Test
    @DisplayName("discoverDatasets returns a single FILE descriptor")
    void discoverDatasets(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("data.csv");
        Files.writeString(csvFile, "x,y\n1,2\n", StandardCharsets.UTF_8);

        var config = new ConnectorConfig.Csv(csvFile.toString(), "data");
        var connector = new CsvDataSourceConnector(config, sparkSessionProvider);

        var datasets = connector.discoverDatasets();

        assertEquals(1, datasets.size());
        assertEquals("data.csv", datasets.getFirst().id());
        assertEquals("data", datasets.getFirst().name());
        assertEquals(DatasetType.FILE, datasets.getFirst().type());
    }

    @Test
    @DisplayName("getMetadata infers columns from CSV header and first data row")
    void getMetadata(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("people.csv");
        Files.writeString(csvFile, "name,age,score\nAlice,30,95.5\n", StandardCharsets.UTF_8);

        var config = new ConnectorConfig.Csv(csvFile.toString(), "people");
        var connector = new CsvDataSourceConnector(config, sparkSessionProvider);

        var meta = connector.getMetadata("people.csv");

        assertEquals("people", meta.name());
        assertEquals(3, meta.columns().size());
        assertEquals("name", meta.columns().get(0).name());
        assertEquals(DataType.STRING, meta.columns().get(0).type());
        assertEquals("age", meta.columns().get(1).name());
        assertEquals(DataType.INTEGER, meta.columns().get(1).type());
        assertEquals("score", meta.columns().get(2).name());
        assertEquals(DataType.DOUBLE, meta.columns().get(2).type());
    }

    @Test
    @DisplayName("getMetadata returns estimated row count")
    void estimatedRowCount(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("rows.csv");
        List<String> lines = new java.util.ArrayList<>();
        lines.add("val");
        for (int i = 0; i < 100; i++) {
            lines.add(String.valueOf(i));
        }
        Files.write(csvFile, lines, StandardCharsets.UTF_8);

        var config = new ConnectorConfig.Csv(csvFile.toString(), "rows");
        var connector = new CsvDataSourceConnector(config, sparkSessionProvider);

        var meta = connector.getMetadata("rows.csv");

        assertEquals(100, meta.estimatedRows());
    }

    @Test
    @DisplayName("createReader returns a reader that produces a Spark DataFrame")
    void createReader(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("read_test.csv");
        Files.writeString(csvFile, "a,b\n1,hello\n2,world\n", StandardCharsets.UTF_8);

        var config = new ConnectorConfig.Csv(csvFile.toString(), "read_test");
        var connector = new CsvDataSourceConnector(config, sparkSessionProvider);

        var reader = connector.createReader("read_test.csv");
        var df = reader.read();

        assertEquals(2, df.count());
        assertEquals(2, df.columns().length);
    }
}
