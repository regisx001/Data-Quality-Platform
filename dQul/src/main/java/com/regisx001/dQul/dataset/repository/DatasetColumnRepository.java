package com.regisx001.dQul.dataset.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regisx001.dQul.dataset.domain.DatasetColumn;

public interface DatasetColumnRepository extends JpaRepository<DatasetColumn, UUID> {

    List<DatasetColumn> findByDatasetId(UUID datasetId);

    Optional<DatasetColumn> findByDatasetIdAndName(UUID datasetId, String name);

    void deleteByDatasetId(UUID datasetId);
}
