package com.regisx001.dQul.compute.spark;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SparkPropertiesTest {

    @Test
    @DisplayName("Default values are set correctly")
    void defaults() {
        var props = new SparkProperties();

        assertTrue(props.isEnabled());
        assertEquals("dQul Compute Engine", props.getAppName());
        assertEquals("local[*]", props.getMaster());
        assertEquals("localhost", props.getDriverHost());
        assertEquals("2g", props.getDriverMemory());
        assertEquals("WARN", props.getLogLevel());
        assertFalse(props.getUi().isEnabled());
        assertTrue(props.getSql().isAdaptiveEnabled());
        assertEquals("UTC", props.getSql().getSessionTimezone());
    }

    @Test
    @DisplayName("Setters update values correctly")
    void setters() {
        var props = new SparkProperties();
        props.setAppName("Custom App");
        props.setMaster("spark://master:7077");
        props.setExtraConfig(Map.of("spark.some.config", "value"));

        assertEquals("Custom App", props.getAppName());
        assertEquals("spark://master:7077", props.getMaster());
        assertEquals("value", props.getExtraConfig().get("spark.some.config"));
    }
}
