package com.regisx001.dQul.validation;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regisx001.dQul.validation.QualityRule;
import com.regisx001.dQul.validation.RuleCategory;
import com.regisx001.dQul.metadata.Dataset;
import com.regisx001.dQul.validation.RuleSeverity;

public interface QualityRuleRepository extends JpaRepository<QualityRule, UUID> {

    List<QualityRule> findByDatasetId(UUID datasetId);

    List<QualityRule> findByDatasetIdAndEnabled(UUID datasetId, boolean enabled);

    List<QualityRule> findByCategory(RuleCategory category);

    List<QualityRule> findBySeverity(RuleSeverity severity);

    List<QualityRule> findByEnabled(boolean enabled);

    long countByDatasetId(UUID datasetId);

    long countByDatasetIdAndEnabled(UUID datasetId, boolean enabled);
}
