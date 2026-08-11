package com.regisx001.dQul.dataset.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.regisx001.dQul.dataset.domain.ColumnProfile;
import com.regisx001.dQul.dataset.domain.Dataset;
import com.regisx001.dQul.dataset.domain.DatasetColumn;
import com.regisx001.dQul.dataset.dto.ColumnProfileResultDto;
import com.regisx001.dQul.compute.dto.DatasetProfileCompletedEvent;
import com.regisx001.dQul.dataset.dto.TableProfileResultDto;
import com.regisx001.dQul.dataset.repository.ColumnProfileRepository;
import com.regisx001.dQul.dataset.repository.DatasetColumnRepository;
import com.regisx001.dQul.dataset.repository.DatasetRepository;
import com.regisx001.dQul.storage.minio.MinioStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProfileResultProcessorService {

    private static final Logger log = LoggerFactory.getLogger(ProfileResultProcessorService.class);

    private final DatasetRepository datasetRepository;
    private final DatasetColumnRepository columnRepository;
    private final ColumnProfileRepository profileRepository;
    private final MinioStorageService minioStorageService;
    private final ObjectMapper objectMapper;

    public ProfileResultProcessorService(DatasetRepository datasetRepository,
                                         DatasetColumnRepository columnRepository,
                                         ColumnProfileRepository profileRepository,
                                         MinioStorageService minioStorageService) {
        this.datasetRepository = datasetRepository;
        this.columnRepository = columnRepository;
        this.profileRepository = profileRepository;
        this.minioStorageService = minioStorageService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Transactional
    public void processProfileResult(DatasetProfileCompletedEvent event) {
        if (event == null || event.getDatasetId() == null) {
            log.warn("Received invalid DatasetProfileCompletedEvent: {}", event);
            return;
        }

        if (!"SUCCESS".equalsIgnoreCase(event.getStatus())) {
            log.error("Dataset profiling failed for profileId={}, datasetId={}: {}",
                    event.getProfileId(), event.getDatasetId(), event.getErrorMessage());
            return;
        }

        log.info("Processing successful dataset profile for datasetId={}, profileId={}",
                event.getDatasetId(), event.getProfileId());

        Optional<Dataset> datasetOpt = datasetRepository.findById(event.getDatasetId());
        if (datasetOpt.isEmpty()) {
            log.error("Dataset with ID {} not found in database! Cannot save profiling results.", event.getDatasetId());
            return;
        }

        Dataset dataset = datasetOpt.get();

        // 1. Fetch JSON profile content from S3/MinIO
        TableProfileResultDto profileResult = fetchProfileResultFromS3(event.getS3ResultUri());
        if (profileResult == null) {
            log.error("Could not fetch or parse profile JSON from S3 URI: {}", event.getS3ResultUri());
            return;
        }

        // 2. Update Dataset metadata
        dataset.setRowCount(profileResult.getRowCount());
        dataset.setLastValidated(LocalDateTime.now());
        datasetRepository.save(dataset);

        // 3. Map existing columns by name
        Map<String, DatasetColumn> existingColumns = dataset.getColumns().stream()
                .collect(Collectors.toMap(DatasetColumn::getName, Function.identity(), (c1, c2) -> c1));

        // 4. Save ColumnProfiles and auto-sync missing DatasetColumns
        for (ColumnProfileResultDto colProfileDto : profileResult.getColumnProfiles()) {
            String colName = colProfileDto.getColumnName();

            DatasetColumn column = existingColumns.get(colName);
            if (column == null) {
                log.info("Auto-syncing new column '{}' (dataType: {}) for datasetId={}",
                        colName, colProfileDto.getDataType(), dataset.getId());

                column = DatasetColumn.builder()
                        .name(colName)
                        .dataType(colProfileDto.getDataType() != null ? colProfileDto.getDataType() : "string")
                        .isNullable(true)
                        .isPrimaryKey(false)
                        .dataset(dataset)
                        .build();

                column = columnRepository.save(column);
                dataset.getColumns().add(column);
                existingColumns.put(colName, column);
            }

            ColumnProfile columnProfile = ColumnProfile.builder()
                    .column(column)
                    .nullCount(colProfileDto.getNullCount() != null ? colProfileDto.getNullCount() : 0L)
                    .nullPercentage(colProfileDto.getNullPercentage() != null ? colProfileDto.getNullPercentage() : 0.0)
                    .distinctCount(colProfileDto.getDistinctCount() != null ? colProfileDto.getDistinctCount() : 0L)
                    .minValue(colProfileDto.getMinValue())
                    .maxValue(colProfileDto.getMaxValue())
                    .avgValue(colProfileDto.getAvgValue())
                    .profiledAt(colProfileDto.getProfiledAt() != null ? colProfileDto.getProfiledAt() : LocalDateTime.now())
                    .build();

            profileRepository.save(columnProfile);
        }

        log.info("Successfully persisted profiling results for datasetId={}, total columns profiled={}",
                dataset.getId(), profileResult.getColumnProfiles().size());
    }

    private TableProfileResultDto fetchProfileResultFromS3(String s3Uri) {
        if (s3Uri == null || !s3Uri.startsWith("s3a://")) {
            log.error("Invalid s3Uri format: {}", s3Uri);
            return null;
        }

        try {
            // Parse s3a://bucket/objectKey
            String pathWithoutScheme = s3Uri.substring(6); // remove "s3a://"
            int slashIdx = pathWithoutScheme.indexOf('/');
            if (slashIdx == -1) {
                log.error("Cannot parse bucket and key from s3Uri: {}", s3Uri);
                return null;
            }

            String bucket = pathWithoutScheme.substring(0, slashIdx);
            String objectKey = pathWithoutScheme.substring(slashIdx + 1);

            try (InputStream is = minioStorageService.getObjectInputStream(bucket, objectKey)) {
                return objectMapper.readValue(is, TableProfileResultDto.class);
            }
        } catch (Exception e) {
            log.error("Failed to read profile result JSON from S3 URI {}: {}", s3Uri, e.getMessage(), e);
            return null;
        }
    }
}
