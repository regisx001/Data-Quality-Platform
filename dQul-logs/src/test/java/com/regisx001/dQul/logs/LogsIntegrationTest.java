package com.regisx001.dQul.logs;

import com.regisx001.dQul.logs.config.KafkaConfig;
import com.regisx001.dQul.logs.domain.LogEntry;
import com.regisx001.dQul.logs.dto.LogIngestionDto;
import com.regisx001.dQul.logs.repository.LogEntryRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 3, topics = {KafkaConfig.LOGS_TOPIC, KafkaConfig.LOGS_DLT_TOPIC})
class LogsIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private LogEntryRepository logEntryRepository;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Test
    void produceConsumeQuery_roundTrip() throws Exception {
        String traceId = "it-" + UUID.randomUUID();
        LogIngestionDto dto = LogIngestionDto.builder()
                .traceId(traceId)
                .serviceName("it-svc")
                .logLevel("ERROR")
                .category("INGESTION")
                .message("round trip event")
                .build();

        kafkaTemplate.send(KafkaConfig.LOGS_TOPIC, traceId, dto);

        LogEntry entry = null;
        for (int i = 0; i < 60 && entry == null; i++) {
            entry = logEntryRepository.findByTraceId(traceId, PageRequest.of(0, 1))
                    .stream().findFirst().orElse(null);
            if (entry == null) {
                Thread.sleep(200);
            }
        }

        assertThat(entry).isNotNull();
        assertThat(entry.getTraceId()).isEqualTo(traceId);
        assertThat(entry.getServiceName()).isEqualTo("it-svc");
        assertThat(entry.getLogLevel()).isEqualTo("ERROR");
        assertThat(entry.getCategory()).isEqualTo("INGESTION");
        assertThat(entry.getMessage()).isEqualTo("round trip event");
    }

    @Test
    void invalidEvent_missingMessage_routesToDlt() throws Exception {
        String key = "it-invalid-" + UUID.randomUUID();
        // Valid JSON object, but missing the required "message" -> LogValidationException -> DLT
        LogIngestionDto invalid = LogIngestionDto.builder()
                .traceId("x")
                .logLevel("INFO")
                .message(null)
                .build();
        kafkaTemplate.send(KafkaConfig.LOGS_TOPIC, key, invalid);

        Consumer<String, String> consumer = createStringConsumer("it-dlt-verify");
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, KafkaConfig.LOGS_DLT_TOPIC);

        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(15));
        assertThat(records.records(KafkaConfig.LOGS_DLT_TOPIC))
                .anyMatch(r -> key.equals(r.key()));
        consumer.close();
    }

    private Consumer<String, String> createStringConsumer(String groupId) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(
                embeddedKafka.getBrokersAsString(), groupId, "false");
        return new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new StringDeserializer()).createConsumer();
    }
}
