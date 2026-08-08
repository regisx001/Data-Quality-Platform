package com.regisx001.dQul.logs.service;

import com.regisx001.dQul.logs.domain.LogEntry;
import com.regisx001.dQul.logs.dto.analytics.LogAnalyticsDto;
import com.regisx001.dQul.logs.dto.analytics.LogAnalyticsRequest;
import com.regisx001.dQul.logs.dto.analytics.LatencyAnalyticsDto;
import com.regisx001.dQul.logs.dto.analytics.LevelAnalyticsDto;
import com.regisx001.dQul.logs.repository.LogEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultLogAnalyticsServiceTest {

    @Mock
    private LogEntryRepository repository;

    @InjectMocks
    private DefaultLogAnalyticsService service;

    private static LogEntry entry(String level, long execMs, Integer status, String traceId) {
        return LogEntry.builder()
                .serviceName("svc-a")
                .category("API")
                .logLevel(level)
                .message("some message value 42")
                .path("/api/health")
                .httpMethod("GET")
                .statusCode(status)
                .executionTimeMs(execMs)
                .traceId(traceId)
                .userId("u1")
                .userEmail("u1@x.com")
                .timestamp(Instant.parse("2026-08-08T10:00:00Z"))
                .build();
    }

    private void stubLogs(List<LogEntry> logs) {
        Page<LogEntry> page = new PageImpl<>(logs);
        when(repository.findAll((org.springframework.data.jpa.domain.Specification<LogEntry>) any(),
                any(Pageable.class))).thenReturn(page);
    }

    @Test
    void analyze_aggregatesCoreDimensions() {
        stubLogs(List.of(
                entry("INFO", 100L, 200, "t1"),
                entry("WARN", 200L, 400, "t1"),
                entry("ERROR", 400L, 500, "t2"),
                entry("INFO", 100L, 200, "t2")));

        LogAnalyticsRequest req = LogAnalyticsRequest.builder()
                .granularity("PT1H")
                .build();

        LogAnalyticsDto dto = service.analyze(req);

        assertThat(dto.getTotalLogs()).isEqualTo(4);
        assertThat(dto.getVolume().getTimeSeries()).isNotEmpty();
        assertThat(dto.getVolume().getMaxLogsInBucket()).isEqualTo(4);

        LevelAnalyticsDto levels = dto.getLevels();
        assertThat(levels.getInfoCount()).isEqualTo(2);
        assertThat(levels.getErrorCount()).isEqualTo(1);
        assertThat(levels.getErrorRatePercentage()).isEqualTo(25.0);

        assertThat(dto.getServices()).hasSize(1);
        assertThat(dto.getServices().get(0).getServiceName()).isEqualTo("svc-a");
        assertThat(dto.getServices().get(0).getErrorCount()).isEqualTo(1);

        assertThat(dto.getCategories()).hasSize(1);
        assertThat(dto.getCategories().get(0).getCategory()).isEqualTo("API");

        assertThat(dto.getHttp().getTotalRequests()).isEqualTo(4);
        assertThat(dto.getHttp().getCount5xx()).isEqualTo(1);
        assertThat(dto.getHttp().getCount4xx()).isEqualTo(1);
        assertThat(dto.getHttp().getEndpoints()).hasSize(1);

        LatencyAnalyticsDto lat = dto.getLatency();
        assertThat(lat.getSampleCount()).isEqualTo(4);
        assertThat(lat.getAverageMs()).isEqualTo(200.0);
        assertThat(lat.getMaxMs()).isEqualTo(400.0);

        assertThat(dto.getErrorSignatures()).hasSize(1);
        assertThat(dto.getErrorSignatures().get(0).getCount()).isEqualTo(1);
        assertThat(dto.getTraces().getUniqueTraces()).isEqualTo(2);
        assertThat(dto.getUsers()).containsKey("u1");
    }

    @Test
    void analyze_normalizesRecurringErrorsIntoSingleSignature() {
        stubLogs(List.of(
                entry("ERROR", 100L, 500, "t1"),
                entry("ERROR", 100L, 500, "t2")));

        LogAnalyticsDto dto = service.analyze(LogAnalyticsRequest.builder().build());

        assertThat(dto.getErrorSignatures()).hasSize(1);
        assertThat(dto.getErrorSignatures().get(0).getCount()).isEqualTo(2);
        // numeric noise collapsed to <num>
        assertThat(dto.getErrorSignatures().get(0).getSignature()).contains("<num>");
    }

    @Test
    void analyze_returnsEmptyTraceWhenNoTraceIds() {
        List<LogEntry> logs = List.of(
                LogEntry.builder().serviceName("s").category("C").logLevel("INFO")
                        .message("m").timestamp(Instant.now()).build());
        stubLogs(logs);

        LogAnalyticsDto dto = service.analyze(LogAnalyticsRequest.builder().build());

        assertThat(dto.getTraces().getUniqueTraces()).isZero();
    }

    @Test
    void analyze_rejectsFromAfterTo() {
        LogAnalyticsRequest req = LogAnalyticsRequest.builder()
                .from(Instant.parse("2026-08-08T12:00:00Z"))
                .to(Instant.parse("2026-08-08T10:00:00Z"))
                .build();

        assertThatThrownBy(() -> service.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from");
    }

    @Test
    void analyze_rejectsInvalidGranularity() {
        stubLogs(List.of());

        LogAnalyticsRequest req = LogAnalyticsRequest.builder()
                .granularity("bogus")
                .build();

        assertThatThrownBy(() -> service.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("granularity");
    }
}
