package com.regisx001.dQul.compute.spark;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SparkProviderUnitTest {

    private static SparkSession sparkSession;
    private static SparkProvider sparkProvider;

    @BeforeAll
    static void setUp() {
        SparkConf conf = new SparkConf()
                .setAppName("SparkProviderUnitTest")
                .setMaster("local[1]")
                .set("spark.ui.enabled", "false");
        sparkSession = SparkSession.builder().config(conf).getOrCreate();
        sparkProvider = new SparkProvider(sparkSession);
    }

    @AfterAll
    static void tearDown() {
        if (sparkSession != null)
            sparkSession.stop();
    }

    @Test
    @DisplayName("getSparkSession returns the injected session")
    void getSparkSession() {
        assertSame(sparkSession, sparkProvider.getSparkSession());
    }

    @Test
    @DisplayName("isSparkActive returns true for active session")
    void isSparkActive() {
        assertTrue(sparkProvider.isSparkActive());
    }

    @Test
    @DisplayName("executeSql runs a simple query and returns results")
    void executeSql() {
        var df = sparkProvider.executeSql("SELECT 1 AS val");

        assertEquals(1, df.count());
        assertEquals("val", df.columns()[0]);
    }

    @Test
    @DisplayName("readDataset with text format reads lines")
    void readDataset() {
        var df = sparkProvider.readDataset("text", "src/test/resources/test-data/sample_words.txt", null);

        assertNotNull(df);
        assertTrue(df.count() > 0);
    }
}
