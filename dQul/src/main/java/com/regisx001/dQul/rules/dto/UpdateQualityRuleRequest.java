package com.regisx001.dQul.rules.dto;

import com.regisx001.dQul.rules.domain.RuleCategory;
import com.regisx001.dQul.rules.domain.RuleSeverity;

public record UpdateQualityRuleRequest(
        String name,
        String description,
        RuleCategory category,
        RuleSeverity severity,
        String expectation,
        Boolean enabled,
        String target,
        String conditionExpression
) {
}
