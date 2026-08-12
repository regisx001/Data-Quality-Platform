package com.regisx001.dQul.compute.io.storage;

import com.regisx001.dQul.compute.dto.TableProfileDto;

/**
 * Abstraction for saving dataset profiling result documents to persistent object storage (S3, MinIO, HDFS, etc.).
 */
public interface ProfileStorageService {

    /**
     * Persists the computed TableProfileDto document.
     *
     * @param profileDto the dataset profiling results
     * @return the destination S3 / storage URI (e.g. s3a://bucket/profiles/id.json)
     */
    String saveProfileResult(TableProfileDto profileDto);
}
