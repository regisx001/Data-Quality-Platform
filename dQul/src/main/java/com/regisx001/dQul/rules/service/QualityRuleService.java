package com.regisx001.dQul.rules.service;

import java.util.List;
import java.util.UUID;

import com.regisx001.dQul.rules.domain.QualityRule;
import com.regisx001.dQul.rules.domain.RuleCategory;
import com.regisx001.dQul.rules.domain.RuleSeverity;
import com.regisx001.dQul.rules.dto.CreateQualityRuleRequest;
import com.regisx001.dQul.rules.dto.UpdateQualityRuleRequest;

public interface QualityRuleService {

    QualityRule createRule(CreateQualityRuleRequest request);

    QualityRule getRuleById(UUID id);

    List<QualityRule> getRules(UUID datasetId, RuleCategory category, RuleSeverity severity, Boolean enabled);

    List<QualityRule> getRulesByDatasetId(UUID datasetId);

    QualityRule updateRule(UUID id, UpdateQualityRuleRequest request);

    QualityRule toggleRule(UUID id);

    void deleteRule(UUID id);
}
