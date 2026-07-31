package com.regisx001.dQul.connector.api;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConnectionTestResultTest {

    @Test
    @DisplayName("Static success factory creates a successful result")
    void success() {
        var result = ConnectionTestResult.success("Connected OK", 42L);

        assertTrue(result.success());
        assertEquals("Connected OK", result.message());
        assertEquals(42L, result.latencyMs());
    }

    @Test
    @DisplayName("Static failure factory creates a failed result")
    void failure() {
        var result = ConnectionTestResult.failure("Connection refused", 7L);

        assertFalse(result.success());
        assertEquals("Connection refused", result.message());
        assertEquals(7L, result.latencyMs());
    }

    @Test
    @DisplayName("notApplicable returns a singleton successful result")
    void notApplicable() {
        var a = ConnectionTestResult.notApplicable();
        var b = ConnectionTestResult.notApplicable();

        assertTrue(a.success());
        assertEquals("Connection test not applicable for this datasource type", a.message());
        assertEquals(0L, a.latencyMs());
        assertSame(a, b, "notApplicable should return the same singleton instance");
    }
}
