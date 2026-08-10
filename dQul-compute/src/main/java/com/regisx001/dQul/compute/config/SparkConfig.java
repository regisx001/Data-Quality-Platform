package com.regisx001.dQul.compute.config;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spark.enabled", havingValue = "true", matchIfMissing = true)
public class SparkConfig {

    private static final Logger log = LoggerFactory.getLogger(SparkConfig.class);

    private final SparkProperties properties;

    public SparkConfig(SparkProperties properties) {
        this.properties = properties;
    }

    @Bean(destroyMethod = "stop")
    public SparkSession sparkSession() {
        log.info("Initializing SparkSession: master={}, appName={}", properties.getMaster(), properties.getAppName());

        SparkConf conf = new SparkConf()
                .setMaster(properties.getMaster())
                .setAppName(properties.getAppName())
                .set("spark.ui.enabled", String.valueOf(properties.isUiEnabled()))
                .set("spark.driver.host", properties.getDriverHost())
                .set("spark.driver.bindAddress", properties.getDriverBindAddress())
                .set("spark.driver.memory", properties.getDriverMemory())
                .set("spark.executor.memory", properties.getExecutorMemory())
                .set("spark.executor.cores", String.valueOf(properties.getExecutorCores()))
                .set("spark.default.parallelism", String.valueOf(properties.getDefaultParallelism()))
                .set("spark.sql.shuffle.partitions", String.valueOf(properties.getShufflePartitions()));

        return SparkSession.builder()
                .config(conf)
                .getOrCreate();
    }

    @Bean
    public JavaSparkContext javaSparkContext(SparkSession sparkSession) {
        return new JavaSparkContext(sparkSession.sparkContext());
    }
}
