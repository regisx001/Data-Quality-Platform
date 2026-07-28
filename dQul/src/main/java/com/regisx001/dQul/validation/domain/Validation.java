package com.regisx001.dQul.validation.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.regisx001.dQul.dataset.domain.Dataset;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
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
@Table(name = "validations")
public class Validation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ValidationTrigger trigger;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ValidationStatus status;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    @Builder.Default
    private long totalRules = 0;

    @Column(nullable = false)
    @Builder.Default
    private long passedRules = 0;

    @Column(nullable = false)
    @Builder.Default
    private long failedRules = 0;

    @Column(nullable = false)
    @Builder.Default
    private long totalFindings = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id", nullable = false)
    private Dataset dataset;

    @OneToMany(mappedBy = "validation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ValidationRuleSnapshot> ruleSnapshots = new ArrayList<>();

    @OneToMany(mappedBy = "validation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Finding> findings = new ArrayList<>();

    public void addRuleSnapshot(ValidationRuleSnapshot snapshot) {
        ruleSnapshots.add(snapshot);
        snapshot.setValidation(this);
    }

    public void addFinding(Finding finding) {
        findings.add(finding);
        finding.setValidation(this);
    }
}
