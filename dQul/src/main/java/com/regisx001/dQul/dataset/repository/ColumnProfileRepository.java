package com.regisx001.dQul.dataset.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regisx001.dQul.dataset.domain.ColumnProfile;

public interface ColumnProfileRepository extends JpaRepository<ColumnProfile, UUID> {

    List<ColumnProfile> findByColumnId(UUID columnId);

    Optional<ColumnProfile> findFirstByColumnIdOrderByProfiledAtDesc(UUID columnId);

    List<ColumnProfile> findByColumnDatasetIdOrderByProfiledAtDesc(UUID datasetId);
}
