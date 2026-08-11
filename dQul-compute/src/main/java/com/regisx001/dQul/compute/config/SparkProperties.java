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
    private String jarsPackages = "org.apache.hadoop:hadoop-aws:3.3.4,com.amazonaws:aws-java-sdk-bundle:1.12.261";

    private S3 s3 = new S3();

    @Data
    public static class S3 {
        private String endpoint = "http://localhost:21001";
        private String accessKey = "minioadmin";
        private String secretKey = "minioadmin123";
        private boolean pathStyleAccess = true;
        private String sslEnabled = "false";
    }
}
