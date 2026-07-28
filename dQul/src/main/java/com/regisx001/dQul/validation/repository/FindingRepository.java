package com.regisx001.dQul.validation.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regisx001.dQul.validation.domain.Finding;
import com.regisx001.dQul.rules.domain.RuleSeverity;

public interface FindingRepository extends JpaRepository<Finding, UUID> {

    List<Finding> findByValidationId(UUID validationId);

    List<Finding> findByQualityRuleId(UUID qualityRuleId);

    List<Finding> findBySeverity(RuleSeverity severity);

    List<Finding> findByValidationIdAndSeverity(UUID validationId, RuleSeverity severity);

    long countByValidationId(UUID validationId);

    long countByValidationIdAndSeverity(UUID validationId, RuleSeverity severity);

    long countByQualityRuleId(UUID qualityRuleId);
}
