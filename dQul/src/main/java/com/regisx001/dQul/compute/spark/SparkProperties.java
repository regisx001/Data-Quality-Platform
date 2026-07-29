package com.regisx001.dQul.compute.spark;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Data
@ConfigurationProperties(prefix = "spark")
public class SparkProperties {

    /**
     * Whether to enable the Spark compute engine.
     */
    private boolean enabled = true;

    /**
     * Application name for the Spark context.
     */
    private String appName = "dQul Compute Engine";

    /**
     * Spark master URL.
     */
    private String master = "local[*]";

    /**
     * Host address for the Spark driver.
     */
    private String driverHost = "localhost";

    /**
     * Bind address for the Spark driver.
     */
    private String driverBindAddress = "127.0.0.1";

    /**
     * Warehouse directory for Spark SQL.
     */
    private String warehouseDirectory = "./spark-warehouse";

    /**
     * Amount of memory to allocate for the Spark driver.
     */
    private String driverMemory = "2g";

    /**
     * Amount of memory to allocate per executor.
     */
    private String executorMemory = "2g";

    /**
     * Number of cores to allocate per executor.
     */
    private int executorCores = 2;

    /**
     * Default number of parallel tasks.
     */
    private int defaultParallelism = 4;

    /**
     * Number of shuffle partitions.
     */
    private int shufflePartitions = 8;

    /**
     * Spark log level.
     */
    private String logLevel = "WARN";

    /**
     * Spark UI configuration.
     */
    private Ui ui = new Ui();

    /**
     * Spark SQL configuration.
     */
    private Sql sql = new Sql();

    /**
     * Serializer class for Spark.
     */
    private String serializer = "org.apache.spark.serializer.JavaSerializer";

    /**
     * Extra Spark configuration properties.
     */
    private Map<String, String> extraConfig;

    @Data
    public static class Ui {

        /**
         * Whether to enable the Spark UI.
         */
        private boolean enabled = false;

        /**
         * Port for the Spark UI.
         */
        private int port = 4040;
    }

    @Data
    public static class Sql {

        /**
         * Whether to enable adaptive query execution.
         */
        private boolean adaptiveEnabled = true;

        /**
         * Whether to enable ANSI SQL compliance.
         */
        private boolean ansiEnabled = true;

        /**
         * Whether Spark SQL is case-sensitive.
         */
        private boolean caseSensitive = false;

        /**
         * Session time zone for Spark SQL.
         */
        private String sessionTimezone = "UTC";

        /**
         * Broadcast timeout in seconds.
         */
        private int broadcastTimeout = 300;
    }
}
