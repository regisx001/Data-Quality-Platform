package com.regisx001.dQul.rules.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.regisx001.dQul.common.exception.GlobalExceptionHandler;
import com.regisx001.dQul.dataset.domain.Dataset;
import com.regisx001.dQul.rules.domain.QualityRule;
import com.regisx001.dQul.rules.domain.RuleCategory;
import com.regisx001.dQul.rules.domain.RuleSeverity;
import com.regisx001.dQul.rules.dto.CreateQualityRuleRequest;
import com.regisx001.dQul.rules.dto.UpdateQualityRuleRequest;
import com.regisx001.dQul.rules.exception.RuleNotFoundException;
import com.regisx001.dQul.rules.service.QualityRuleService;

@ExtendWith(MockitoExtension.class)
class QualityRuleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private QualityRuleService qualityRuleService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private UUID ruleId;
    private UUID datasetId;
    private Dataset mockDataset;
    private QualityRule mockRule;

    @BeforeEach
    void setUp() {
        QualityRuleController controller = new QualityRuleController(qualityRuleService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        ruleId = UUID.randomUUID();
        datasetId = UUID.randomUUID();

        mockDataset = Dataset.builder()
                .id(datasetId)
                .name("test_orders.csv")
                .type("CSV")
                .build();

        mockRule = QualityRule.builder()
                .id(ruleId)
                .name("Customer ID Not Null")
                .description("Ensures customer_id is never null")
                .category(RuleCategory.COMPLETENESS)
                .severity(RuleSeverity.HIGH)
                .expectation("is_not_null")
                .enabled(true)
                .target("customer_id")
                .conditionExpression("customer_id IS NOT NULL")
                .dataset(mockDataset)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/rules - creates rule and returns 201 Created")
    void createRule_Success() throws Exception {
        CreateQualityRuleRequest request = new CreateQualityRuleRequest(
                "Customer ID Not Null",
                "Ensures customer_id is never null",
                RuleCategory.COMPLETENESS,
                RuleSeverity.HIGH,
                "is_not_null",
                true,
                "customer_id",
                "customer_id IS NOT NULL",
                datasetId
        );

        when(qualityRuleService.createRule(any(CreateQualityRuleRequest.class))).thenReturn(mockRule);

        mockMvc.perform(post("/api/v1/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ruleId.toString()))
                .andExpect(jsonPath("$.name").value("Customer ID Not Null"))
                .andExpect(jsonPath("$.category").value("COMPLETENESS"))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.target").value("customer_id"))
                .andExpect(jsonPath("$.datasetId").value(datasetId.toString()))
                .andExpect(jsonPath("$.datasetName").value("test_orders.csv"));

        verify(qualityRuleService).createRule(any(CreateQualityRuleRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/rules - returns 400 Bad Request when required fields missing")
    void createRule_ValidationFailure() throws Exception {
        CreateQualityRuleRequest invalidRequest = new CreateQualityRuleRequest(
                "", // Blank name
                "Missing name test",
                null, // Null category
                RuleSeverity.LOW,
                "", // Blank expectation
                true,
                "", // Blank target
                null,
                null // Null datasetId
        );

        mockMvc.perform(post("/api/v1/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/rules - returns list of rules")
    void getAllRules_Success() throws Exception {
        when(qualityRuleService.getRules(eq(datasetId), any(), any(), any()))
                .thenReturn(List.of(mockRule));

        mockMvc.perform(get("/api/v1/rules")
                        .param("datasetId", datasetId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Customer ID Not Null"));
    }

    @Test
    @DisplayName("GET /api/v1/rules/{id} - returns rule by id")
    void getRuleById_Success() throws Exception {
        when(qualityRuleService.getRuleById(ruleId)).thenReturn(mockRule);

        mockMvc.perform(get("/api/v1/rules/{id}", ruleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ruleId.toString()))
                .andExpect(jsonPath("$.name").value("Customer ID Not Null"));
    }

    @Test
    @DisplayName("GET /api/v1/rules/{id} - returns 404 Not Found when rule does not exist")
    void getRuleById_NotFound() throws Exception {
        when(qualityRuleService.getRuleById(ruleId))
                .thenThrow(new RuleNotFoundException("id", ruleId));

        mockMvc.perform(get("/api/v1/rules/{id}", ruleId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RULE_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/v1/rules/dataset/{datasetId} - returns rules for dataset")
    void getRulesByDatasetId_Success() throws Exception {
        when(qualityRuleService.getRulesByDatasetId(datasetId)).thenReturn(List.of(mockRule));

        mockMvc.perform(get("/api/v1/rules/dataset/{datasetId}", datasetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].datasetId").value(datasetId.toString()));
    }

    @Test
    @DisplayName("PUT /api/v1/rules/{id} - updates rule and returns 200 OK")
    void updateRule_Success() throws Exception {
        UpdateQualityRuleRequest updateRequest = new UpdateQualityRuleRequest(
                "Updated Rule Name",
                "Updated description",
                RuleCategory.VALIDITY,
                RuleSeverity.CRITICAL,
                "range_between",
                true,
                "amount",
                "amount > 0"
        );

        mockRule.setName("Updated Rule Name");
        mockRule.setSeverity(RuleSeverity.CRITICAL);

        when(qualityRuleService.updateRule(eq(ruleId), any(UpdateQualityRuleRequest.class)))
                .thenReturn(mockRule);

        mockMvc.perform(put("/api/v1/rules/{id}", ruleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Rule Name"))
                .andExpect(jsonPath("$.severity").value("CRITICAL"));
    }

    @Test
    @DisplayName("PATCH /api/v1/rules/{id}/toggle - toggles enabled state")
    void toggleRule_Success() throws Exception {
        mockRule.setEnabled(false);
        when(qualityRuleService.toggleRule(ruleId)).thenReturn(mockRule);

        mockMvc.perform(patch("/api/v1/rules/{id}/toggle", ruleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @DisplayName("DELETE /api/v1/rules/{id} - deletes rule and returns 204 No Content")
    void deleteRule_Success() throws Exception {
        doNothing().when(qualityRuleService).deleteRule(ruleId);

        mockMvc.perform(delete("/api/v1/rules/{id}", ruleId))
                .andExpect(status().isNoContent());

        verify(qualityRuleService).deleteRule(ruleId);
    }
}
