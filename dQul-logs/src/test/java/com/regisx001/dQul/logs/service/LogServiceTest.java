package com.regisx001.dQul.logs.service;

import com.regisx001.dQul.logs.common.error.LogValidationException;
import com.regisx001.dQul.logs.domain.LogEntry;
import com.regisx001.dQul.logs.dto.LogIngestionDto;
import com.regisx001.dQul.logs.dto.LogQueryResultDto;
import com.regisx001.dQul.logs.dto.LogStatsDto;
import com.regisx001.dQul.logs.repository.LogEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogServiceTest {

    @Mock
    private LogEntryRepository repository;

    @InjectMocks
    private LogService logService;

    // --- save / normalize ---

    @Test
    void saveLog_persistsNormalizedEntry() {
        LogIngestionDto dto = LogIngestionDto.builder()
                .traceId("t-1")
                .serviceName("  svc  ")
                .logLevel("error")
                .category("validation")
                .message("boom")
                .timestamp(Instant.parse("2026-08-04T12:00:00Z"))
                .build();
        when(repository.save(any(LogEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        LogEntry result = logService.saveLog(dto);

        assertThat(result.getServiceName()).isEqualTo("svc");
        assertThat(result.getLogLevel()).isEqualTo("ERROR");
        assertThat(result.getCategory()).isEqualTo("VALIDATION");
        assertThat(result.getMessage()).isEqualTo("boom");
        assertThat(result.getTimestamp()).isEqualTo(Instant.parse("2026-08-04T12:00:00Z"));
        verify(repository).save(any(LogEntry.class));
    }

    @Test
    void saveLog_appliesDefaults() {
        LogIngestionDto dto = LogIngestionDto.builder().message("hello").build();
        when(repository.save(any(LogEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        LogEntry result = logService.saveLog(dto);

        assertThat(result.getServiceName()).isEqualTo("unknown-service");
        assertThat(result.getLogLevel()).isEqualTo("INFO");
        assertThat(result.getCategory()).isEqualTo("INTERNAL_LOG");
        assertThat(result.getTimestamp()).isNotNull();
    }

    @Test
    void saveLog_nullDto_throws() {
        assertThatThrownBy(() -> logService.saveLog(null))
                .isInstanceOf(LogValidationException.class)
                .hasMessageContaining("null");
    }

    @Test
    void saveLog_blankMessage_throws() {
        LogIngestionDto dto = LogIngestionDto.builder().message("   ").build();
        assertThatThrownBy(() -> logService.saveLog(dto))
                .isInstanceOf(LogValidationException.class)
                .hasMessageContaining("message");
    }

    @Test
    void saveLog_invalidLogLevel_throws() {
        LogIngestionDto dto = LogIngestionDto.builder().message("x").logLevel("BANANA").build();
        assertThatThrownBy(() -> logService.saveLog(dto))
                .isInstanceOf(LogValidationException.class)
                .hasMessageContaining("Invalid logLevel");
    }

    @Test
    void saveLog_categoryTooLong_throws() {
        LogIngestionDto dto = LogIngestionDto.builder().message("x").category("A".repeat(33)).build();
        assertThatThrownBy(() -> logService.saveLog(dto))
                .isInstanceOf(LogValidationException.class)
                .hasMessageContaining("32");
    }

    // --- query / get ---

    @Test
    void getLogById_delegatesToRepository() {
        UUID id = UUID.randomUUID();
        LogEntry entry = LogEntry.builder().id(id).build();
        when(repository.findById(id)).thenReturn(Optional.of(entry));

        assertThat(logService.getLogById(id)).contains(entry);
    }

    @Test
    void queryLogs_delegatesWithPageable() {
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        LogQueryResultDto result = logService.queryLogs("search", "ERROR", "svc", "cat", "trace", pageable);

        verify(repository).findAll(any(Specification.class), eq(pageable));
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // --- stats ---

    @Test
    void getLogStats_computesTotalsAndRate() {
        when(repository.count()).thenReturn(100L);
        when(repository.countByLogLevel("ERROR")).thenReturn(10L);
        when(repository.countByLogLevel("FATAL")).thenReturn(5L);
        when(repository.countByLogLevel("WARN")).thenReturn(20L);
        when(repository.countByLogLevel("INFO")).thenReturn(65L);
        when(repository.getAverageExecutionTimeSince(any(Instant.class))).thenReturn(250.5);
        when(repository.countLogsByService()).thenReturn(Collections.singletonList(new Object[] { "svc", 100L }));
        when(repository.countLogsByCategory())
                .thenReturn(Collections.singletonList(new Object[] { "VALIDATION", 100L }));

        LogStatsDto stats = logService.getLogStats();

        assertThat(stats.getTotalLogs()).isEqualTo(100L);
        assertThat(stats.getErrorCount()).isEqualTo(15L);
        assertThat(stats.getWarnCount()).isEqualTo(20L);
        assertThat(stats.getInfoCount()).isEqualTo(65L);
        assertThat(stats.getErrorRatePercentage()).isEqualTo(15.0);
        assertThat(stats.getAverageLatencyMs()).isEqualTo(250.5);
        assertThat(stats.getLogsByService()).isEqualTo(Map.of("svc", 100L));
        assertThat(stats.getLogsByCategory()).isEqualTo(Map.of("VALIDATION", 100L));
    }

    // --- purge ---

    @Test
    void purgeLogsOlderThan_delegatesToRepository() {
        logService.purgeLogsOlderThan(30);
        verify(repository).deleteByTimestampBefore(any(Instant.class));
    }
}
