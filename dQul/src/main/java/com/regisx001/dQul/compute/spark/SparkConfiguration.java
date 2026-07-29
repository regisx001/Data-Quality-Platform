package com.regisx001.dQul.compute.spark;

import org.apache.spark.sql.SparkSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SparkProperties.class)
@ConditionalOnProperty(name = "spark.enabled", havingValue = "true", matchIfMissing = true)
public class SparkConfiguration {

    private final SparkProperties properties;

    public SparkConfiguration(SparkProperties properties) {
        this.properties = properties;
    }

    @Bean(destroyMethod = "close")
    public SparkSession sparkSession() {
        SparkSession.Builder builder = SparkSession.builder()
                .appName(properties.getAppName())
                .master(properties.getMaster())
                .config("spark.driver.host", properties.getDriverHost())
                .config("spark.driver.bindAddress", properties.getDriverBindAddress())
                .config("spark.sql.warehouse.dir", properties.getWarehouseDirectory())
                .config("spark.driver.memory", properties.getDriverMemory())
                .config("spark.executor.memory", properties.getExecutorMemory())
                .config("spark.executor.cores", String.valueOf(properties.getExecutorCores()))
                .config("spark.default.parallelism", String.valueOf(properties.getDefaultParallelism()))
                .config("spark.sql.shuffle.partitions", String.valueOf(properties.getShufflePartitions()));

        if (properties.getSerializer() != null && !properties.getSerializer().isBlank()) {
            builder.config("spark.serializer", properties.getSerializer());
        }

        // --- UI configuration ---
        SparkProperties.Ui ui = properties.getUi();
        boolean uiEnabled = ui != null && ui.isEnabled();
        builder.config("spark.ui.enabled", String.valueOf(uiEnabled));
        builder.config("spark.ui.port", uiEnabled ? String.valueOf(ui.getPort()) : "0");

        // --- SQL configuration ---
        SparkProperties.Sql sql = properties.getSql();
        builder.config("spark.sql.adaptive.enabled", String.valueOf(sql.isAdaptiveEnabled()))
                .config("spark.sql.ansi.enabled", String.valueOf(sql.isAnsiEnabled()))
                .config("spark.sql.caseSensitive", String.valueOf(sql.isCaseSensitive()))
                .config("spark.sql.session.timeZone", sql.getSessionTimezone())
                .config("spark.sql.broadcastTimeout", String.valueOf(sql.getBroadcastTimeout()));

        // --- Extra configuration ---
        if (properties.getExtraConfig() != null) {
            properties.getExtraConfig().forEach(builder::config);
        }

        SparkSession spark = builder.getOrCreate();

        // Set log level after session creation (safely catch logging framework mismatches)
        try {
            if (properties.getLogLevel() != null && !properties.getLogLevel().isBlank()) {
                spark.sparkContext().setLogLevel(properties.getLogLevel());
            }
        } catch (Throwable e) {
            // Ignore logging framework cast exceptions when SLF4J bridge is used by Spring Boot
        }

        return spark;
    }

}
