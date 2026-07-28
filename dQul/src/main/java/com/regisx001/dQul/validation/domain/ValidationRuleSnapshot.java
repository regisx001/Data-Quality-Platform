package com.regisx001.dQul.validation.domain;

import java.util.UUID;

import com.regisx001.dQul.rules.domain.RuleCategory;
import com.regisx001.dQul.rules.domain.RuleSeverity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "validation_rule_snapshots")
public class ValidationRuleSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID qualityRuleId;

    @Column(nullable = false)
    private String ruleName;

    @Column(columnDefinition = "TEXT")
    private String ruleDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleSeverity severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String expectation;

    @Column(nullable = false)
    private String target;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validation_id", nullable = false)
    private Validation validation;
}
