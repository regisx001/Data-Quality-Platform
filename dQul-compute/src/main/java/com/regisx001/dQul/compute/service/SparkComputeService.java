package com.regisx001.dQul.compute.service;

import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SparkComputeService {

    private static final Logger log = LoggerFactory.getLogger(SparkComputeService.class);

    private final SparkSession sparkSession;

    public SparkComputeService(SparkSession sparkSession) {
        this.sparkSession = sparkSession;
    }

    public boolean isSparkActive() {
        return sparkSession != null && !sparkSession.sparkContext().isStopped();
    }

    public String getSparkVersion() {
        return isSparkActive() ? sparkSession.version() : "INACTIVE";
    }
}
