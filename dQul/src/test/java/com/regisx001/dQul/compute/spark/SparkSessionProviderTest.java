package com.regisx001.dQul.compute.spark;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SparkSessionProviderTest {

    @Test
    @DisplayName("get returns the injected SparkSession")
    void get() {
        var session = mock(SparkSession.class);
        var provider = new SparkSessionProvider(session);

        assertSame(session, provider.get());
    }

    @Test
    @DisplayName("get returns the same instance on multiple calls")
    void get_sameInstance() {
        var session = mock(SparkSession.class);
        var provider = new SparkSessionProvider(session);

        assertSame(provider.get(), provider.get());
    }
}
