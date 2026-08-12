package com.regisx001.dQul.compute.engine.streaming;

/**
 * Interface abstraction for real-time Spark Structured Streaming log aggregations.
 */
public interface RealtimeLogStreamEngine {

    /**
     * Starts the Spark Structured Streaming query consuming live log events from Kafka
     * and publishing windowed aggregated metrics to Redis Pub/Sub.
     */
    void startLogStreamingQuery();

    /**
     * Stops the active Spark Structured Streaming query gracefully.
     */
    void stopLogStreamingQuery();

    /**
     * Checks if the real-time streaming query is currently running.
     *
     * @return true if active, false otherwise
     */
    boolean isStreamingActive();
}
