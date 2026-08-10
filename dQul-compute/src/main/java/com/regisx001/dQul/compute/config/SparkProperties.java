package com.regisx001.dQul.compute.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spark")
public class SparkProperties {
    private boolean enabled = true;
    private boolean uiEnabled = false;
    private String master = "local[*]";
    private String appName = "dQul-compute-service";
    private String driverHost = "127.0.0.1";
    private String driverBindAddress = "127.0.0.1";
    private String driverMemory = "1g";
    private String executorMemory = "1g";
    private int executorCores = 2;
    private int defaultParallelism = 4;
    private int shufflePartitions = 4;
}
