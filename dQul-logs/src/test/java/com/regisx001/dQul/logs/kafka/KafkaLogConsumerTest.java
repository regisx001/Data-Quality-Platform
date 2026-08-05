package com.regisx001.dQul.logs.kafka;

import com.regisx001.dQul.logs.config.KafkaConfig;
import com.regisx001.dQul.logs.dto.LogIngestionDto;
import com.regisx001.dQul.logs.service.LogService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 3, topics = {KafkaConfig.LOGS_TOPIC, KafkaConfig.LOGS_DLT_TOPIC})
class KafkaLogConsumerTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @MockitoBean
    private LogService logService;

    @Test
    void validEvent_isDeliveredToService() {
        LogIngestionDto dto = LogIngestionDto.builder()
                .traceId("t-valid")
                .serviceName("svc")
                .logLevel("INFO")
                .category("VALIDATION")
                .message("hello")
                .build();

        kafkaTemplate.send(KafkaConfig.LOGS_TOPIC, "t-valid", dto);

        verify(logService, timeout(10_000)).saveLog(any(LogIngestionDto.class));
    }

    @Test
    void malformedJson_isRoutedToDlt() {
        // A JSON string (not an object) cannot be deserialized to LogIngestionDto -> DLT
        kafkaTemplate.send(KafkaConfig.LOGS_TOPIC, "t-poison", "\"not-an-object\"");

        Consumer<String, String> consumer = createStringConsumer("dlt-verify-group");
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, KafkaConfig.LOGS_DLT_TOPIC);

        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        assertThat(records.records(KafkaConfig.LOGS_DLT_TOPIC))
                .anyMatch(r -> "t-poison".equals(r.key()));
        consumer.close();
    }

    private Consumer<String, String> createStringConsumer(String groupId) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(
                embeddedKafka.getBrokersAsString(), groupId, "false");
        return new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new StringDeserializer()).createConsumer();
    }
}
