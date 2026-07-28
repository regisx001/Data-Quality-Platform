package com.regisx001.dQul.validation.domain;
import com.regisx001.dQul.rules.domain.QualityRule;

import java.time.LocalDateTime;
import java.util.UUID;

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
@Table(name = "findings")
public class Finding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleSeverity severity;

    @Column(nullable = false)
    @Builder.Default
    private long affectedRecords = 0;

    @Column(nullable = false)
    private LocalDateTime detectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validation_id", nullable = false)
    private Validation validation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quality_rule_id", nullable = false)
    private QualityRule qualityRule;
}
