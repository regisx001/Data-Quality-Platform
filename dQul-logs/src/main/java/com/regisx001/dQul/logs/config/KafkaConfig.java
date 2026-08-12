package com.regisx001.dQul.logs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regisx001.dQul.logs.dto.LogIngestionDto;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Configuration
public class KafkaConfig {

    public static final String LOGS_TOPIC = "platform-logs-topic";
    public static final String LOGS_DLT_TOPIC = LOGS_TOPIC + ".DLT";

    @Bean
    public NewTopic platformLogsTopic() {
        return TopicBuilder.name(LOGS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic logsAggregateRequestTopic() {
        return TopicBuilder.name("dqul.logs.aggregate.request")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic platformLogsDltTopic() {
        return TopicBuilder.name(LOGS_DLT_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Consumer factory for log events. Uses the application's Jackson
     * {@link ObjectMapper} (registered with java.time support) so ISO-8601
     * timestamps deserialize to {@link java.time.Instant}, and always maps the
     * JSON payload to {@link LogIngestionDto} regardless of producer type headers.
     */
    @Bean
    public ConsumerFactory<String, LogIngestionDto> kafkaConsumerFactory(
            KafkaProperties kafkaProperties, ObjectMapper objectMapper) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        // Inner JSON deserializer maps the payload to LogIngestionDto regardless
        // of producer type headers, using the application ObjectMapper (java.time
        // support) so ISO-8601 timestamps deserialize to Instant.
        JsonDeserializer<LogIngestionDto> jsonDeserializer =
                new JsonDeserializer<>(LogIngestionDto.class, objectMapper);
        jsonDeserializer.setUseTypeHeaders(false);
        jsonDeserializer.addTrustedPackages("*");
        // Wrap in ErrorHandlingDeserializer so that deserialization failures
        // (e.g. a String value that cannot map to LogIngestionDto) surface as a
        // recoverable DeserializationException. This lets the container's
        // DefaultErrorHandler retry and then route poison messages to the DLT,
        // instead of throwing IllegalStateException and wedging the consumer at
        // the same offset forever.
        ErrorHandlingDeserializer<LogIngestionDto> valueDeserializer =
                new ErrorHandlingDeserializer<>(jsonDeserializer);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
    }

    /**
     * Listener container factory for the log consumer. Uses a
     * {@link DeadLetterPublishingRecoverer} so that poison messages (bad JSON,
     * validation failures, transient processing errors) are retried up to 3 times
     * and then routed to the dead-letter topic instead of crashing the consumer.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, LogIngestionDto> kafkaListenerContainerFactory(
            ConsumerFactory<String, LogIngestionDto> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, LogIngestionDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(LOGS_DLT_TOPIC, record.partition()));
        factory.setCommonErrorHandler(new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3)));
        return factory;
    }

    /**
     * Consumer factory for Spark batch completion events. Deserializes
     * JSON messages from topic dqul.logs.aggregate.result to LogsAggregationCompletedEvent.
     */
    @Bean
    public ConsumerFactory<String, com.regisx001.dQul.logs.dto.batch.LogsAggregationCompletedEvent> batchCompletionConsumerFactory(
            KafkaProperties kafkaProperties, ObjectMapper objectMapper) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        JsonDeserializer<com.regisx001.dQul.logs.dto.batch.LogsAggregationCompletedEvent> jsonDeserializer =
                new JsonDeserializer<>(com.regisx001.dQul.logs.dto.batch.LogsAggregationCompletedEvent.class, objectMapper);
        jsonDeserializer.setUseTypeHeaders(false);
        jsonDeserializer.addTrustedPackages("*");
        ErrorHandlingDeserializer<com.regisx001.dQul.logs.dto.batch.LogsAggregationCompletedEvent> valueDeserializer =
                new ErrorHandlingDeserializer<>(jsonDeserializer);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, com.regisx001.dQul.logs.dto.batch.LogsAggregationCompletedEvent> batchCompletionKafkaListenerContainerFactory(
            ConsumerFactory<String, com.regisx001.dQul.logs.dto.batch.LogsAggregationCompletedEvent> batchCompletionConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, com.regisx001.dQul.logs.dto.batch.LogsAggregationCompletedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(batchCompletionConsumerFactory);
        return factory;
    }
}
