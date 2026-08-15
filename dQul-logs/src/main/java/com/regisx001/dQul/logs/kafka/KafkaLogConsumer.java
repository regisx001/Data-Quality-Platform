package com.regisx001.dQul.logs.kafka;

import com.regisx001.dQul.logs.config.KafkaConfig;
import com.regisx001.dQul.logs.dto.LogIngestionDto;
import com.regisx001.dQul.logs.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaLogConsumer {

    private final LogService logService;

    @KafkaListener(
        topics = KafkaConfig.LOGS_TOPIC,
        groupId = "${spring.kafka.consumer.group-id:dqul-logs-group}"
    )
    public void consumeLogEvent(LogIngestionDto dto) {
        if (dto == null) {
            log.warn("Received null log event; skipping");
            return;
        }
        try {
            logService.saveLog(dto);
            log.debug("Persisted log event for traceId [{}]", dto.getTraceId());
        } catch (Exception e) {
            log.error("Internal error persisting log event for traceId [{}]: {}", dto.getTraceId(), e.getMessage(), e);
            throw e;
        }
    }
}
