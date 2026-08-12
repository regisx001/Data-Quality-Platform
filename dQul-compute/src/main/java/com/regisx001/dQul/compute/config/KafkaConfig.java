package com.regisx001.dQul.compute.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${dqul.kafka.topics.profile-request:dqul.dataset.profile.request}")
    private String profileRequestTopic;

    @Value("${dqul.kafka.topics.profile-result:dqul.dataset.profile.result}")
    private String profileResultTopic;

    @Value("${dqul.kafka.topics.logs-aggregate-request:dqul.logs.aggregate.request}")
    private String logsAggregateRequestTopic;

    @Value("${dqul.kafka.topics.logs-aggregate-result:dqul.logs.aggregate.result}")
    private String logsAggregateResultTopic;

    @Bean
    public NewTopic profileRequestTopic() {
        return TopicBuilder.name(profileRequestTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic profileResultTopic() {
        return TopicBuilder.name(profileResultTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic logsAggregateRequestTopic() {
        return TopicBuilder.name(logsAggregateRequestTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic logsAggregateResultTopic() {
        return TopicBuilder.name(logsAggregateResultTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
