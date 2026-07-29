package com.regisx001.dQul.compute.spark;

import org.apache.spark.sql.SparkSession;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around the singleton {@link SparkSession} bean.
 *
 * <p>
 * Services should inject this provider rather than {@link SparkSession}
 * directly, keeping the dependency contract focused and mock-friendly.
 */
@Component
public class SparkSessionProvider {

    private final SparkSession sparkSession;

    public SparkSessionProvider(SparkSession sparkSession) {
        this.sparkSession = sparkSession;
    }

    /**
     * Returns the application-wide {@link SparkSession}.
     */
    public SparkSession get() {
        return sparkSession;
    }
}
