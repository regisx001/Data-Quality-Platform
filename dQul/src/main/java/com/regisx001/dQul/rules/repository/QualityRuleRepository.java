package com.regisx001.dQul.rules.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regisx001.dQul.rules.domain.QualityRule;
import com.regisx001.dQul.rules.domain.RuleCategory;
import com.regisx001.dQul.dataset.domain.Dataset;
import com.regisx001.dQul.rules.domain.RuleSeverity;

public interface QualityRuleRepository extends JpaRepository<QualityRule, UUID> {

    List<QualityRule> findByDatasetId(UUID datasetId);

    List<QualityRule> findByDatasetIdAndEnabled(UUID datasetId, boolean enabled);

    List<QualityRule> findByCategory(RuleCategory category);

    List<QualityRule> findBySeverity(RuleSeverity severity);

    List<QualityRule> findByEnabled(boolean enabled);

    long countByDatasetId(UUID datasetId);

    long countByDatasetIdAndEnabled(UUID datasetId, boolean enabled);
}
