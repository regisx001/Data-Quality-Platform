package com.regisx001.dQul.rules.dto;

import java.util.UUID;

import com.regisx001.dQul.rules.domain.RuleCategory;
import com.regisx001.dQul.rules.domain.RuleSeverity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateQualityRuleRequest(
        @NotBlank(message = "Rule name is required")
        String name,

        String description,

        @NotNull(message = "Rule category is required")
        RuleCategory category,

        RuleSeverity severity,

        @NotBlank(message = "Expectation definition is required")
        String expectation,

        Boolean enabled,

        @NotBlank(message = "Target column or entity is required")
        String target,

        String conditionExpression,

        @NotNull(message = "Dataset ID is required")
        UUID datasetId
) {
    public RuleSeverity getEffectiveSeverity() {
        return severity != null ? severity : RuleSeverity.MEDIUM;
    }

    public boolean isEffectiveEnabled() {
        return enabled != null ? enabled : true;
    }
}
