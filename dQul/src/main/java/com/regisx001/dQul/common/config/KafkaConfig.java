package com.regisx001.dQul.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

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
    public NewTopic platformLogsDltTopic() {
        return TopicBuilder.name(LOGS_DLT_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

}
