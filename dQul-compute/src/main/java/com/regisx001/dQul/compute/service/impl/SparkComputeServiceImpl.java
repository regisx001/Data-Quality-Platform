package com.regisx001.dQul.compute.service.impl;

import com.regisx001.dQul.compute.service.SparkComputeService;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SparkComputeServiceImpl implements SparkComputeService {

    private static final Logger log = LoggerFactory.getLogger(SparkComputeServiceImpl.class);

    private final SparkSession sparkSession;

    public SparkComputeServiceImpl(SparkSession sparkSession) {
        this.sparkSession = sparkSession;
    }

    @Override
    public boolean isSparkActive() {
        return sparkSession != null && !sparkSession.sparkContext().isStopped();
    }

    @Override
    public String getSparkVersion() {
        return isSparkActive() ? sparkSession.version() : "INACTIVE";
    }
}
