package com.regisx001.dQul.logs.controller;

import com.regisx001.dQul.logs.domain.LogEntry;
import com.regisx001.dQul.logs.dto.LogQueryResultDto;
import com.regisx001.dQul.logs.dto.LogStatsDto;
import com.regisx001.dQul.logs.dto.analytics.LogAnalyticsDto;
import com.regisx001.dQul.logs.kafka.LogsAggregationKafkaProducer;
import com.regisx001.dQul.logs.service.LogAnalyticsService;
import com.regisx001.dQul.logs.service.LogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LogController.class)
class LogControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private LogService logService;

        @MockitoBean
        private LogAnalyticsService logAnalyticsService;

        @MockitoBean
        private LogsAggregationKafkaProducer aggregationKafkaProducer;

        @MockitoBean
        private com.regisx001.dQul.logs.service.BatchLogMetricService batchLogMetricService;

        @Test
        void queryLogs_returnsPage() throws Exception {
                LogEntry entry = LogEntry.builder()
                                .id(UUID.randomUUID())
                                .serviceName("svc")
                                .logLevel("INFO")
                                .category("CAT")
                                .message("hi")
                                .timestamp(Instant.now())
                                .build();
                List<LogEntry> entries = List.of(entry);
                LogQueryResultDto result = LogQueryResultDto.builder()
                                .content(entries)
                                .totalElements(1L)
                                .build();
                when(logService.queryLogs(any(), any(), any(), any(), any(), any())).thenReturn(result);

                mockMvc.perform(get("/api/v1/logs").param("level", "INFO"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].message").value("hi"))
                                .andExpect(jsonPath("$.page").value(0))
                                .andExpect(jsonPath("$.size").value(20))
                                .andExpect(jsonPath("$.totalElements").value(1))
                                .andExpect(jsonPath("$.totalPages").value(1))
                                .andExpect(jsonPath("$.first").value(true))
                                .andExpect(jsonPath("$.last").value(true))
                                .andExpect(jsonPath("$.hasNext").value(false))
                                .andExpect(jsonPath("$.hasPrevious").value(false));
        }

        @Test
        void queryLogs_rejectsNegativePage() throws Exception {
                mockMvc.perform(get("/api/v1/logs").param("page", "-1"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        void queryLogs_rejectsOversizedPage() throws Exception {
                mockMvc.perform(get("/api/v1/logs").param("size", "101"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        void getLogById_returnsEntry() throws Exception {
                UUID id = UUID.randomUUID();
                LogEntry entry = LogEntry.builder()
                                .id(id).serviceName("svc").message("x").timestamp(Instant.now()).build();
                when(logService.getLogById(id)).thenReturn(Optional.of(entry));

                mockMvc.perform(get("/api/v1/logs/{id}", id))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(id.toString()));
        }

        @Test
        void getLogById_notFound_returnsApiErrorEnvelope() throws Exception {
                UUID id = UUID.randomUUID();
                when(logService.getLogById(id)).thenReturn(Optional.empty());

                mockMvc.perform(get("/api/v1/logs/{id}", id))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.error").value("Not Found"))
                                .andExpect(jsonPath("$.message").value("Log entry not found: " + id));
        }

        @Test
        void getStats_returnsStats() throws Exception {
                LogStatsDto stats = LogStatsDto.builder()
                                .totalLogs(10)
                                .errorCount(2)
                                .errorRatePercentage(20.0)
                                .logsByService(Map.of("svc", 10L))
                                .build();
                when(logService.getLogStats()).thenReturn(stats);

                mockMvc.perform(get("/api/v1/logs/stats"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalLogs").value(10))
                                .andExpect(jsonPath("$.errorRatePercentage").value(20.0));
        }

        @Test
        void purgeLogs_returnsSuccess() throws Exception {
                mockMvc.perform(delete("/api/v1/logs/purge").param("days", "7"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SUCCESS"));
                verify(logService).purgeLogsOlderThan(7);
        }

        @Test
        void purgeLogs_invalidDays_returns400() throws Exception {
                mockMvc.perform(delete("/api/v1/logs/purge").param("days", "0"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        void getAnalytics_returnsEnvelope() throws Exception {
                LogAnalyticsDto result = LogAnalyticsDto.builder()
                                .totalLogs(25)
                                .build();
                when(logAnalyticsService.analyze(any())).thenReturn(result);

                mockMvc.perform(get("/api/v1/logs/analytics")
                                .param("granularity", "PT1H")
                                .param("serviceName", "svc"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalLogs").value(25));

                verify(logAnalyticsService).analyze(any());
        }

        @Test
        void getAnalytics_invalidFrom_returns400() throws Exception {
                mockMvc.perform(get("/api/v1/logs/analytics").param("from", "not-a-time"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400));
        }
}
