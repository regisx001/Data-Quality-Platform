package com.regisx001.dQul.compute.service;

/**
 * Service abstraction for managing and checking the status of SparkSession lifecycle.
 */
public interface SparkComputeService {

    /**
     * Checks if the underlying SparkSession is initialized and active.
     *
     * @return true if Spark is active, false otherwise
     */
    boolean isSparkActive();

    /**
     * Retrieves the version string of Apache Spark engine.
     *
     * @return Spark version string
     */
    String getSparkVersion();
}
