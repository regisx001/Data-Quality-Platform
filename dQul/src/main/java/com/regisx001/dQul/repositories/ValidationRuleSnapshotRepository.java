package com.regisx001.dQul.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regisx001.dQul.domain.entities.ValidationRuleSnapshot;

public interface ValidationRuleSnapshotRepository extends JpaRepository<ValidationRuleSnapshot, UUID> {

    List<ValidationRuleSnapshot> findByValidationId(UUID validationId);

    List<ValidationRuleSnapshot> findByQualityRuleId(UUID qualityRuleId);

    long countByValidationId(UUID validationId);
}
