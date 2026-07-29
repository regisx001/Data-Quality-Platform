package com.regisx001.dQul.compute.spark.demo;

import com.regisx001.dQul.compute.spark.SparkProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class SparkDemoControllerTest {

    private static SparkSession sparkSession;
    private static SparkDemoController controller;

    @BeforeAll
    static void setUp() {
        SparkConf conf = new SparkConf()
                .setAppName("SparkDemoControllerTest")
                .setMaster("local[*]")
                .set("spark.ui.enabled", "false");

        sparkSession = SparkSession.builder()
                .config(conf)
                .getOrCreate();

        SparkProvider sparkProvider = new SparkProvider(sparkSession);
        controller = new SparkDemoController(sparkProvider);
    }

    @AfterAll
    static void tearDown() {
        if (sparkSession != null) {
            sparkSession.stop();
        }
    }

    @Test
    @DisplayName("Demo Endpoint 1: GET /api/v1/spark/demo/status")
    void testGetStatusDemo() {
        ResponseEntity<SparkDemoController.SparkStatusResponse> response = controller.getStatus();

        assertNotNull(response.getBody());
        assertTrue(response.getBody().isActive());
        assertEquals("SparkDemoControllerTest", response.getBody().getAppName());

        log.info("Status Response: {}", response.getBody());
    }

    @Test
    @DisplayName("Demo Endpoint 2: POST /api/v1/spark/demo/wordcount")
    void testWordCountDemo() {
        SparkDemoController.WordCountRequest request = new SparkDemoController.WordCountRequest();
        request.setText("Spark engine is powerful. Spark is fast and scalable.");

        ResponseEntity<Map<String, Object>> response = controller.runWordCountDemo(request);

        assertNotNull(response.getBody());
        assertEquals("SUCCESS", response.getBody().get("status"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");

        assertNotNull(results);
        assertFalse(results.isEmpty());

        log.info("Word Count Response Results: {}", results);
    }

    @Test
    @DisplayName("Demo Endpoint 3: GET /api/v1/spark/demo/sql")
    void testSqlDemo() {
        ResponseEntity<Map<String, Object>> response = controller.runSqlDemo();

        assertNotNull(response.getBody());
        assertEquals("SUCCESS", response.getBody().get("status"));
        assertEquals("dQul-Spark Engine", response.getBody().get("engine"));

        log.info("SQL Response Output: {}", response.getBody());
    }
}
