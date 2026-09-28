package com.regisx001.dQul.rules.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.regisx001.dQul.rules.domain.QualityRule;
import com.regisx001.dQul.rules.domain.RuleCategory;
import com.regisx001.dQul.rules.domain.RuleSeverity;
import com.regisx001.dQul.rules.dto.CreateQualityRuleRequest;
import com.regisx001.dQul.rules.dto.QualityRuleResponse;
import com.regisx001.dQul.rules.dto.UpdateQualityRuleRequest;
import com.regisx001.dQul.rules.service.QualityRuleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping({"/api/v1/rules", "/api/v1/quality-rules"})
public class QualityRuleController {

    private final QualityRuleService qualityRuleService;

    public QualityRuleController(QualityRuleService qualityRuleService) {
        this.qualityRuleService = qualityRuleService;
    }

    /**
     * Create a new Quality Rule.
     * POST /api/v1/rules or /api/v1/quality-rules
     */
    @PostMapping
    public ResponseEntity<QualityRuleResponse> createRule(@Valid @RequestBody CreateQualityRuleRequest request) {
        QualityRule created = qualityRuleService.createRule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(QualityRuleResponse.from(created));
    }

    /**
     * Get all Quality Rules, optionally filtered by datasetId, category, severity, or enabled state.
     * GET /api/v1/rules
     */
    @GetMapping
    public ResponseEntity<List<QualityRuleResponse>> getAllRules(
            @RequestParam(required = false) UUID datasetId,
            @RequestParam(required = false) RuleCategory category,
            @RequestParam(required = false) RuleSeverity severity,
            @RequestParam(required = false) Boolean enabled) {
        List<QualityRule> rules = qualityRuleService.getRules(datasetId, category, severity, enabled);
        List<QualityRuleResponse> response = rules.stream().map(QualityRuleResponse::from).toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Get a single Quality Rule by its ID.
     * GET /api/v1/rules/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<QualityRuleResponse> getRuleById(@PathVariable UUID id) {
        QualityRule rule = qualityRuleService.getRuleById(id);
        return ResponseEntity.ok(QualityRuleResponse.from(rule));
    }

    /**
     * Get all Quality Rules attached to a specific Dataset.
     * GET /api/v1/rules/dataset/{datasetId}
     */
    @GetMapping("/dataset/{datasetId}")
    public ResponseEntity<List<QualityRuleResponse>> getRulesByDatasetId(@PathVariable UUID datasetId) {
        List<QualityRule> rules = qualityRuleService.getRulesByDatasetId(datasetId);
        List<QualityRuleResponse> response = rules.stream().map(QualityRuleResponse::from).toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing Quality Rule.
     * PUT /api/v1/rules/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<QualityRuleResponse> updateRule(
            @PathVariable UUID id,
            @RequestBody UpdateQualityRuleRequest request) {
        QualityRule updated = qualityRuleService.updateRule(id, request);
        return ResponseEntity.ok(QualityRuleResponse.from(updated));
    }

    /**
     * Toggle the enabled/disabled state of a Quality Rule.
     * PATCH /api/v1/rules/{id}/toggle
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<QualityRuleResponse> toggleRule(@PathVariable UUID id) {
        QualityRule toggled = qualityRuleService.toggleRule(id);
        return ResponseEntity.ok(QualityRuleResponse.from(toggled));
    }

    /**
     * Delete a Quality Rule by its ID.
     * DELETE /api/v1/rules/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        qualityRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
