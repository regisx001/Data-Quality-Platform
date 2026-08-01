package com.regisx001.dQul.dataset.service;

import java.util.UUID;

import com.regisx001.dQul.dataset.dto.DataPreviewResult;
import com.regisx001.dQul.dataset.dto.DatasetDetailResponse;

public interface DatasetService {

    /**
     * Gets detailed metadata for a dataset, including its columns and profiling statistics.
     */
    DatasetDetailResponse getDatasetById(UUID id);

    /**
     * Retrieves sample preview rows for a dataset.
     */
    DataPreviewResult getDatasetPreview(UUID id, int limit);

    /**
     * Triggers statistical profiling for a dataset's columns and updates metadata.
     */
    DatasetDetailResponse profileDataset(UUID id);
}
