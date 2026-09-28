package com.regisx001.dQul.rules.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regisx001.dQul.dataset.domain.Dataset;
import com.regisx001.dQul.dataset.exception.DatasetNotFoundException;
import com.regisx001.dQul.dataset.repository.DatasetRepository;
import com.regisx001.dQul.rules.domain.QualityRule;
import com.regisx001.dQul.rules.domain.RuleCategory;
import com.regisx001.dQul.rules.domain.RuleSeverity;
import com.regisx001.dQul.rules.dto.CreateQualityRuleRequest;
import com.regisx001.dQul.rules.dto.UpdateQualityRuleRequest;
import com.regisx001.dQul.rules.exception.InvalidRuleDefinitionException;
import com.regisx001.dQul.rules.exception.RuleNotFoundException;
import com.regisx001.dQul.rules.repository.QualityRuleRepository;

@Service
@Transactional
public class QualityRuleServiceImpl implements QualityRuleService {

    private static final Logger log = LoggerFactory.getLogger(QualityRuleServiceImpl.class);

    private final QualityRuleRepository qualityRuleRepository;
    private final DatasetRepository datasetRepository;

    public QualityRuleServiceImpl(QualityRuleRepository qualityRuleRepository,
                                  DatasetRepository datasetRepository) {
        this.qualityRuleRepository = qualityRuleRepository;
        this.datasetRepository = datasetRepository;
    }

    @Override
    public QualityRule createRule(CreateQualityRuleRequest request) {
        if (request == null) {
            throw new InvalidRuleDefinitionException("Quality rule request body cannot be null");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new InvalidRuleDefinitionException("Quality rule name is required");
        }
        if (request.category() == null) {
            throw new InvalidRuleDefinitionException("Quality rule category is required");
        }
        if (request.expectation() == null || request.expectation().isBlank()) {
            throw new InvalidRuleDefinitionException("Quality rule expectation is required");
        }
        if (request.target() == null || request.target().isBlank()) {
            throw new InvalidRuleDefinitionException("Quality rule target column or entity is required");
        }
        if (request.datasetId() == null) {
            throw new InvalidRuleDefinitionException("Quality rule must be associated with a valid dataset ID");
        }

        Dataset dataset = datasetRepository.findById(request.datasetId())
                .orElseThrow(() -> new DatasetNotFoundException("id", request.datasetId()));

        QualityRule rule = QualityRule.builder()
                .name(request.name().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .category(request.category())
                .severity(request.getEffectiveSeverity())
                .expectation(request.expectation().trim())
                .enabled(request.isEffectiveEnabled())
                .target(request.target().trim())
                .conditionExpression(request.conditionExpression() != null && !request.conditionExpression().isBlank()
                        ? request.conditionExpression().trim()
                        : null)
                .dataset(dataset)
                .build();

        QualityRule saved = qualityRuleRepository.save(rule);
        log.info("Created quality rule '{}' (id: {}) for dataset '{}' (id: {})",
                saved.getName(), saved.getId(), dataset.getName(), dataset.getId());

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public QualityRule getRuleById(UUID id) {
        return qualityRuleRepository.findById(id)
                .orElseThrow(() -> new RuleNotFoundException("id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<QualityRule> getRules(UUID datasetId, RuleCategory category, RuleSeverity severity, Boolean enabled) {
        if (datasetId != null) {
            if (enabled != null) {
                return qualityRuleRepository.findByDatasetIdAndEnabled(datasetId, enabled);
            }
            return qualityRuleRepository.findByDatasetId(datasetId);
        }
        if (category != null) {
            return qualityRuleRepository.findByCategory(category);
        }
        if (severity != null) {
            return qualityRuleRepository.findBySeverity(severity);
        }
        if (enabled != null) {
            return qualityRuleRepository.findByEnabled(enabled);
        }
        return qualityRuleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QualityRule> getRulesByDatasetId(UUID datasetId) {
        return qualityRuleRepository.findByDatasetId(datasetId);
    }

    @Override
    public QualityRule updateRule(UUID id, UpdateQualityRuleRequest request) {
        QualityRule rule = getRuleById(id);

        if (request.name() != null && !request.name().isBlank()) {
            rule.setName(request.name().trim());
        }
        if (request.description() != null) {
            rule.setDescription(request.description().trim());
        }
        if (request.category() != null) {
            rule.setCategory(request.category());
        }
        if (request.severity() != null) {
            rule.setSeverity(request.severity());
        }
        if (request.expectation() != null && !request.expectation().isBlank()) {
            rule.setExpectation(request.expectation().trim());
        }
        if (request.enabled() != null) {
            rule.setEnabled(request.enabled());
        }
        if (request.target() != null && !request.target().isBlank()) {
            rule.setTarget(request.target().trim());
        }
        if (request.conditionExpression() != null) {
            rule.setConditionExpression(request.conditionExpression().isBlank() ? null : request.conditionExpression().trim());
        }

        QualityRule updated = qualityRuleRepository.save(rule);
        log.info("Updated quality rule '{}' (id: {})", updated.getName(), updated.getId());

        return updated;
    }

    @Override
    public QualityRule toggleRule(UUID id) {
        QualityRule rule = getRuleById(id);
        rule.setEnabled(!rule.isEnabled());
        QualityRule saved = qualityRuleRepository.save(rule);
        log.info("Toggled quality rule '{}' (id: {}) to enabled={}", saved.getName(), saved.getId(), saved.isEnabled());
        return saved;
    }

    @Override
    public void deleteRule(UUID id) {
        QualityRule rule = getRuleById(id);
        qualityRuleRepository.delete(rule);
        log.info("Deleted quality rule '{}' (id: {})", rule.getName(), rule.getId());
    }
}
