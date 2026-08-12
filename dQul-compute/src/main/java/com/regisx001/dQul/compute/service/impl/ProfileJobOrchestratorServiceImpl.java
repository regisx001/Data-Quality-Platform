package com.regisx001.dQul.compute.service.impl;

import com.regisx001.dQul.compute.dto.DatasetProfileCompletedEvent;
import com.regisx001.dQul.compute.dto.DatasetProfileRequest;
import com.regisx001.dQul.compute.dto.TableProfileDto;
import com.regisx001.dQul.compute.engine.batch.profiler.DatasetProfilerEngine;
import com.regisx001.dQul.compute.io.reader.DatasetReader;
import com.regisx001.dQul.compute.io.storage.ProfileStorageService;
import com.regisx001.dQul.compute.service.ProfileJobOrchestratorService;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ProfileJobOrchestratorServiceImpl implements ProfileJobOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(ProfileJobOrchestratorServiceImpl.class);

    private final DatasetReader datasetReader;
    private final DatasetProfilerEngine profilerEngine;
    private final ProfileStorageService storageService;

    public ProfileJobOrchestratorServiceImpl(DatasetReader datasetReader,
                                             DatasetProfilerEngine profilerEngine,
                                             ProfileStorageService storageService) {
        this.datasetReader = datasetReader;
        this.profilerEngine = profilerEngine;
        this.storageService = storageService;
    }

    @Override
    public DatasetProfileCompletedEvent executeProfilingJob(DatasetProfileRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("DatasetProfileRequest must not be null");
        }

        log.info("Orchestrating profiling job for profileId={}, datasetId={}", request.getProfileId(), request.getDatasetId());
        long startTime = System.currentTimeMillis();

        try {
            // 1. Read dataset via DatasetReader (I/O)
            Dataset<Row> df = datasetReader.read(request);

            // 2. Profile dataset via DatasetProfilerEngine (Compute)
            TableProfileDto tableProfile = profilerEngine.profile(request, df);

            // 3. Store result via ProfileStorageService (Persistence)
            String s3ResultUri = storageService.saveProfileResult(tableProfile);

            long duration = System.currentTimeMillis() - startTime;

            log.info("Successfully completed profiling job for profileId={} in {} ms", request.getProfileId(), duration);

            return DatasetProfileCompletedEvent.builder()
                    .profileId(request.getProfileId())
                    .datasetId(request.getDatasetId())
                    .status("SUCCESS")
                    .s3ResultUri(s3ResultUri)
                    .rowCount(tableProfile.getRowCount())
                    .columnCount(tableProfile.getColumnCount())
                    .executionDurationMs(duration)
                    .completedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to execute profiling job for profileId={}: {}", request.getProfileId(), e.getMessage(), e);

            return DatasetProfileCompletedEvent.builder()
                    .profileId(request.getProfileId())
                    .datasetId(request.getDatasetId())
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .executionDurationMs(duration)
                    .completedAt(LocalDateTime.now())
                    .build();
        }
    }
}
