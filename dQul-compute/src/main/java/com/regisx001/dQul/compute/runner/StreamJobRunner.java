package com.regisx001.dQul.compute.runner;

import com.regisx001.dQul.compute.engine.streaming.RealtimeLogStreamEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class StreamJobRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StreamJobRunner.class);

    private final RealtimeLogStreamEngine streamEngine;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public StreamJobRunner(RealtimeLogStreamEngine streamEngine) {
        this.streamEngine = streamEngine;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Submitting background task for Spark Real-time Log Streaming query...");
        executorService.submit(() -> {
            try {
                streamEngine.startLogStreamingQuery();
            } catch (Exception e) {
                log.error("Error in background Spark Real-time Log Streaming execution: {}", e.getMessage(), e);
            }
        });
    }
}
