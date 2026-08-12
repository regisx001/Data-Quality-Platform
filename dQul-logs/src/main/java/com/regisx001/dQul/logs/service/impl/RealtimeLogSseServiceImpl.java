package com.regisx001.dQul.logs.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regisx001.dQul.logs.domain.RealtimeLogMetricEntity;
import com.regisx001.dQul.logs.dto.streaming.RealtimeLogMetricsDto;
import com.regisx001.dQul.logs.repository.RealtimeLogMetricRepository;
import com.regisx001.dQul.logs.service.RealtimeLogSseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class RealtimeLogSseServiceImpl implements RealtimeLogSseService, MessageListener {

    public static final String REDIS_REALTIME_CHANNEL = "dqul:logs:realtime:stream";

    private final RealtimeLogMetricRepository metricRepository;
    private final ObjectMapper objectMapper;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    private final RedisMessageListenerContainer redisContainer;

    public RealtimeLogSseServiceImpl(RealtimeLogMetricRepository metricRepository,
                                       ObjectMapper objectMapper,
                                       @org.springframework.beans.factory.annotation.Autowired(required = false) RedisMessageListenerContainer redisContainer) {
        this.metricRepository = metricRepository;
        this.objectMapper = objectMapper;
        this.redisContainer = redisContainer;
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void initRedisListener() {
        try {
            if (redisContainer != null) {
                redisContainer.addMessageListener(this, new ChannelTopic(REDIS_REALTIME_CHANNEL));
                if (!redisContainer.isRunning()) {
                    redisContainer.start();
                }
                log.info("Registered and started Redis Pub/Sub listener on channel '{}' for real-time SSE logs streaming", REDIS_REALTIME_CHANNEL);
            }
        } catch (Exception e) {
            log.warn("Redis is not available for real-time Pub/Sub streaming: {}", e.getMessage());
        }
    }

    @Override
    public SseEmitter subscribeSse() {
        SseEmitter emitter = new SseEmitter(0L); // Infinite timeout for long-lived SSE connections
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data("Realtime log streaming analytics connected"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            RealtimeLogMetricsDto dto = objectMapper.readValue(body, RealtimeLogMetricsDto.class);
            processAndBroadcastMetric(dto);
        } catch (Exception e) {
            log.error("Error processing Redis message on realtime logs channel: {}", e.getMessage(), e);
        }
    }

    @Override
    public void processAndBroadcastMetric(RealtimeLogMetricsDto metricDto) {
        if (metricDto == null) return;

        // 1. Save timeseries snapshot row to PostgreSQL for historical visualization
        try {
            String serviceBreakdownJson = metricDto.getServiceCounts() != null ?
                    objectMapper.writeValueAsString(metricDto.getServiceCounts()) : "{}";

            RealtimeLogMetricEntity entity = RealtimeLogMetricEntity.builder()
                    .windowStart(metricDto.getWindowStart() != null ? metricDto.getWindowStart() : "")
                    .windowEnd(metricDto.getWindowEnd() != null ? metricDto.getWindowEnd() : "")
                    .throughputLogsPerSec(metricDto.getThroughputLogsPerSec() != null ? metricDto.getThroughputLogsPerSec() : 0.0)
                    .totalLogsCount(metricDto.getTotalLogsCount() != null ? metricDto.getTotalLogsCount() : 0L)
                    .infoCount(metricDto.getInfoCount() != null ? metricDto.getInfoCount() : 0L)
                    .warnCount(metricDto.getWarnCount() != null ? metricDto.getWarnCount() : 0L)
                    .errorCount(metricDto.getErrorCount() != null ? metricDto.getErrorCount() : 0L)
                    .debugCount(metricDto.getDebugCount() != null ? metricDto.getDebugCount() : 0L)
                    .avgExecutionTimeMs(metricDto.getAvgExecutionTimeMs())
                    .serviceBreakdown(serviceBreakdownJson)
                    .createdAt(metricDto.getTimestamp() != null ? metricDto.getTimestamp() : Instant.now())
                    .build();

            metricRepository.save(entity);
        } catch (Exception e) {
            log.warn("Failed to persist realtime log metric window to PostgreSQL: {}", e.getMessage());
        }

        // 2. Broadcast to all active SSE client connections
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("LOG_METRICS_UPDATE")
                        .data(metricDto));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }

    @Override
    public List<RealtimeLogMetricsDto> getHistoricalMetrics(int limit) {
        int maxLimit = Math.min(Math.max(limit, 1), 500);
        List<RealtimeLogMetricEntity> entities = metricRepository.findRecentMetrics(PageRequest.of(0, maxLimit));
        List<RealtimeLogMetricsDto> dtos = new ArrayList<>();

        for (RealtimeLogMetricEntity entity : entities) {
            dtos.add(RealtimeLogMetricsDto.builder()
                    .windowStart(entity.getWindowStart())
                    .windowEnd(entity.getWindowEnd())
                    .throughputLogsPerSec(entity.getThroughputLogsPerSec())
                    .totalLogsCount(entity.getTotalLogsCount())
                    .infoCount(entity.getInfoCount())
                    .warnCount(entity.getWarnCount())
                    .errorCount(entity.getErrorCount())
                    .debugCount(entity.getDebugCount())
                    .avgExecutionTimeMs(entity.getAvgExecutionTimeMs())
                    .timestamp(entity.getCreatedAt())
                    .build());
        }

        Collections.reverse(dtos); // Return in chronological order
        return dtos;
    }
}
