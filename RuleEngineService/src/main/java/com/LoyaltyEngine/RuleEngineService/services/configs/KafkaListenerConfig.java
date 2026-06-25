package com.LoyaltyEngine.RuleEngineService.services.configs;

import com.LoyaltyEngine.RuleEngineService.models.eventModels.TransactionCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
@Slf4j
public class KafkaListenerConfig {
    @Value("${spring.kafka.bootstrap-servers:9092}")
    private String bootstrapServers;
    private final KafkaTopicsConfig topicsConfig;
    private final KafkaTemplate<UUID, Object> dlqKafkaTemplate;

    public KafkaListenerConfig(KafkaTopicsConfig topicsConfig, @Qualifier("dlqKafkaTemplate") KafkaTemplate<UUID, Object> dlqKafkaTemplate) {
        this.topicsConfig = topicsConfig;
        this.dlqKafkaTemplate = dlqKafkaTemplate;
    }

    @Bean
    public ConsumerFactory<UUID, TransactionCreatedEvent> transactionCreatedEventModelConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, UUIDDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "*");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new org.apache.kafka.common.serialization.UUIDDeserializer(),
                new JacksonJsonDeserializer<>(TransactionCreatedEvent.class, false)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<UUID, TransactionCreatedEvent> transactionCreatedEventModelConcurrentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, TransactionCreatedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(transactionCreatedEventModelConsumerFactory());
        factory.setCommonErrorHandler(kafkaErrorHandler());

        return factory;
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        ExponentialBackOffWithMaxRetries backoff = new ExponentialBackOffWithMaxRetries(3);
        backoff.setInitialInterval(1000L);
        backoff.setMultiplier(2.0);
        backoff.setMaxInterval(4000L);

        return new DefaultErrorHandler(
                (consumerRecord, e) -> {
                    log.error("Error handling message from topic {} :", consumerRecord.topic(), e);

                    dlqKafkaTemplate.send(
                            consumerRecord.topic() + topicsConfig.getDlqSuffix(),
                            consumerRecord.partition(),
                            (UUID) consumerRecord.key(),
                            consumerRecord.value());
                },
                backoff
        );
    }


}
