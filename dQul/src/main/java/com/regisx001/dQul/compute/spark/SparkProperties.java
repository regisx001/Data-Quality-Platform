package com.regisx001.dQul.compute.spark;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "spark")
public class SparkProperties {

    private String appName = "dQul-Data-Quality-Engine";
    private String master = "local[*]";
    private String driverMemory = "1g";
    private String executorMemory = "1g";
    private boolean uiEnabled = false;
    private Map<String, String> config = new HashMap<>();
}

