package com.regisx001.dQul.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regisx001.dQul.domain.entities.Validation;
import com.regisx001.dQul.domain.enums.ValidationStatus;
import com.regisx001.dQul.domain.enums.ValidationTrigger;

public interface ValidationRepository extends JpaRepository<Validation, UUID> {

    List<Validation> findByDatasetId(UUID datasetId);

    List<Validation> findByDatasetIdOrderByStartedAtDesc(UUID datasetId);

    List<Validation> findByStatus(ValidationStatus status);

    List<Validation> findByTrigger(ValidationTrigger trigger);

    List<Validation> findByDatasetIdAndStatus(UUID datasetId, ValidationStatus status);

    List<Validation> findByStartedAtBetween(LocalDateTime start, LocalDateTime end);

    Validation findTopByDatasetIdOrderByStartedAtDesc(UUID datasetId);
}
