package com.regisx001.dQul.logs.service;

import com.regisx001.dQul.logs.dto.analytics.LogAnalyticsDto;
import com.regisx001.dQul.logs.dto.analytics.LogAnalyticsRequest;
import com.regisx001.dQul.logs.repository.LogEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqlAggregatedLogAnalyticsServiceTest {

    @Mock
    private LogEntryRepository repository;

    @InjectMocks
    private SqlAggregatedLogAnalyticsService service;

    @Test
    void testAnalyze_InvalidWindow() {
        LogAnalyticsRequest req = LogAnalyticsRequest.builder()
                .from(Instant.parse("2026-08-08T12:00:00Z"))
                .to(Instant.parse("2026-08-08T10:00:00Z"))
                .build();

        assertThatThrownBy(() -> service.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from must be before or equal to to");
    }

    @Test
    void testAnalyze_Success() {
        Instant from = Instant.parse("2026-08-08T00:00:00Z");
        Instant to = Instant.parse("2026-08-08T23:59:59Z");

        List<Object[]> levelsList = new ArrayList<>();
        levelsList.add(new Object[]{"INFO", 80L});
        levelsList.add(new Object[]{"ERROR", 20L});
        when(repository.countLogsByLevelAggregated(any(), any(), any(), any(), any()))
                .thenReturn(levelsList);

        List<Object[]> servicesList = new ArrayList<>();
        servicesList.add(new Object[]{"dQul-api", 100L, 20L, 0L, 5L, 150.0, 500.0});
        when(repository.aggregateServices(any(), any(), any(), any(), any()))
                .thenReturn(servicesList);

        List<Object[]> categoriesList = new ArrayList<>();
        categoriesList.add(new Object[]{"API", 100L, 20L, 0L, 150.0});
        when(repository.aggregateCategories(any(), any(), any(), any(), any()))
                .thenReturn(categoriesList);

        when(repository.findLatencies(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(100L, 150L, 200L, 500L));

        List<Object[]> statusList = new ArrayList<>();
        statusList.add(new Object[]{200, 80L});
        statusList.add(new Object[]{500, 200L});
        when(repository.aggregateHttpStatusCodes(any(), any(), any(), any(), any()))
                .thenReturn(statusList);

        List<Object[]> methodList = new ArrayList<>();
        methodList.add(new Object[]{"GET", 100L});
        when(repository.aggregateHttpMethodCounts(any(), any(), any(), any(), any()))
                .thenReturn(methodList);

        List<Object[]> endpointList = new ArrayList<>();
        endpointList.add(new Object[]{"GET", "/api/v1/health", 100L, 20L});
        when(repository.aggregateHttpEndpoints(any(), any(), any(), any(), any()))
                .thenReturn(endpointList);

        when(repository.aggregateUsers(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        LogAnalyticsRequest req = LogAnalyticsRequest.builder()
                .from(from)
                .to(to)
                .build();

        LogAnalyticsDto dto = service.analyze(req);

        assertThat(dto).isNotNull();
        assertThat(dto.getTotalLogs()).isEqualTo(100L);
        assertThat(dto.getLevels().getErrorCount()).isEqualTo(20L);
        assertThat(dto.getLevels().getInfoCount()).isEqualTo(80L);
        assertThat(dto.getServices()).hasSize(1);
        assertThat(dto.getServices().get(0).getServiceName()).isEqualTo("dQul-api");
    }
}
