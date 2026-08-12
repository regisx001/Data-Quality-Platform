package com.regisx001.dQul.logs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regisx001.dQul.logs.domain.RealtimeLogMetricEntity;
import com.regisx001.dQul.logs.dto.streaming.RealtimeLogMetricsDto;
import com.regisx001.dQul.logs.repository.RealtimeLogMetricRepository;
import com.regisx001.dQul.logs.service.impl.RealtimeLogSseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RealtimeLogSseServiceImplTest {

    @Mock
    private RealtimeLogMetricRepository metricRepository;

    @Mock
    private RedisMessageListenerContainer redisContainer;

    private RealtimeLogSseServiceImpl sseService;

    @BeforeEach
    void setUp() {
        sseService = new RealtimeLogSseServiceImpl(metricRepository, new ObjectMapper(), redisContainer);
    }

    @Test
    void testSubscribeSse() {
        SseEmitter emitter = sseService.subscribeSse();
        assertNotNull(emitter);
    }

    @Test
    void testProcessAndBroadcastMetric() {
        RealtimeLogMetricsDto dto = RealtimeLogMetricsDto.builder()
                .windowStart("2026-08-12T16:10:00Z")
                .windowEnd("2026-08-12T16:10:05Z")
                .throughputLogsPerSec(45.0)
                .totalLogsCount(225L)
                .infoCount(200L)
                .warnCount(20L)
                .errorCount(5L)
                .levelCounts(Map.of("INFO", 200L, "WARN", 20L, "ERROR", 5L))
                .serviceCounts(Map.of("auth-service", 100L))
                .avgExecutionTimeMs(65.4)
                .timestamp(Instant.now())
                .build();

        sseService.processAndBroadcastMetric(dto);

        ArgumentCaptor<RealtimeLogMetricEntity> captor = ArgumentCaptor.forClass(RealtimeLogMetricEntity.class);
        verify(metricRepository, times(1)).save(captor.capture());

        RealtimeLogMetricEntity saved = captor.getValue();
        assertEquals("2026-08-12T16:10:00Z", saved.getWindowStart());
        assertEquals("2026-08-12T16:10:05Z", saved.getWindowEnd());
        assertEquals(225L, saved.getTotalLogsCount());
        assertEquals(45.0, saved.getThroughputLogsPerSec());
    }

    @Test
    void testGetHistoricalMetrics() {
        RealtimeLogMetricEntity entity = RealtimeLogMetricEntity.builder()
                .id(UUID.randomUUID())
                .windowStart("2026-08-12T16:10:00Z")
                .windowEnd("2026-08-12T16:10:05Z")
                .throughputLogsPerSec(30.0)
                .totalLogsCount(150L)
                .infoCount(140L)
                .warnCount(10L)
                .createdAt(Instant.now())
                .build();

        when(metricRepository.findRecentMetrics(any(Pageable.class))).thenReturn(List.of(entity));

        List<RealtimeLogMetricsDto> history = sseService.getHistoricalMetrics(10);

        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals(150L, history.get(0).getTotalLogsCount());
    }
}
