package com.regisx001.dQul.compute.spark;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SparkProperties.class)
public class SparkConfiguration {

    @Bean
    public SparkConf sparkConf(SparkProperties sparkProperties) {
        SparkConf conf = new SparkConf()
                .setAppName(sparkProperties.getAppName())
                .setMaster(sparkProperties.getMaster())
                .set("spark.ui.enabled", String.valueOf(sparkProperties.isUiEnabled()));

        if (sparkProperties.getDriverMemory() != null && !sparkProperties.getDriverMemory().isBlank()) {
            conf.set("spark.driver.memory", sparkProperties.getDriverMemory());
        }

        if (sparkProperties.getExecutorMemory() != null && !sparkProperties.getExecutorMemory().isBlank()) {
            conf.set("spark.executor.memory", sparkProperties.getExecutorMemory());
        }

        if (sparkProperties.getConfig() != null) {
            sparkProperties.getConfig().forEach(conf::set);
        }

        return conf;
    }

    @Bean(destroyMethod = "stop")
    public SparkSession sparkSession(SparkConf sparkConf) {
        return SparkSession.builder()
                .config(sparkConf)
                .getOrCreate();
    }
}
