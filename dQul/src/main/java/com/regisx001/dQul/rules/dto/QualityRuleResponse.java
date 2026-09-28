package com.regisx001.dQul.rules.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.regisx001.dQul.rules.domain.QualityRule;
import com.regisx001.dQul.rules.domain.RuleCategory;
import com.regisx001.dQul.rules.domain.RuleSeverity;

public record QualityRuleResponse(
        UUID id,
        String name,
        String description,
        RuleCategory category,
        RuleSeverity severity,
        String expectation,
        boolean enabled,
        String target,
        String conditionExpression,
        LocalDateTime lastExecuted,
        UUID datasetId,
        String datasetName
) {
    public static QualityRuleResponse from(QualityRule rule) {
        if (rule == null) {
            return null;
        }
        return new QualityRuleResponse(
                rule.getId(),
                rule.getName(),
                rule.getDescription(),
                rule.getCategory(),
                rule.getSeverity(),
                rule.getExpectation(),
                rule.isEnabled(),
                rule.getTarget(),
                rule.getConditionExpression(),
                rule.getLastExecuted(),
                rule.getDataset() != null ? rule.getDataset().getId() : null,
                rule.getDataset() != null ? rule.getDataset().getName() : null
        );
    }
}
