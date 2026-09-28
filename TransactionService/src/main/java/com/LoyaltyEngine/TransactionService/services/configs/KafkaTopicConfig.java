package com.LoyaltyEngine.TransactionService.services.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaTopicConfig {
    @Value("${kafka.topics.transaction-handled}")
    private String transactionHandledTopic;

    @Value("${kafka.topics.points-failed}")
    private String pointsFailedTopic;

    @Value("${kafka.topics.transaction-created}")
    private String transactionCreatedTopic;

    @Bean
    public KafkaAdmin.NewTopics newTopic() {
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name(transactionHandledTopic).partitions(1).replicas(1).build(),
                TopicBuilder.name(transactionCreatedTopic).partitions(1).replicas(1).build(),
                TopicBuilder.name(pointsFailedTopic).partitions(1).replicas(1).build()
        );
    }

}
