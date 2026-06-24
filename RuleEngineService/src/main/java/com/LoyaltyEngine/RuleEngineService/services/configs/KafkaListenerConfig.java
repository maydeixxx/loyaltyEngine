package com.LoyaltyEngine.RuleEngineService.services.configs;

import com.LoyaltyEngine.RuleEngineService.models.eventModels.TransactionCreatedEventModel;
import lombok.RequiredArgsConstructor;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class KafkaListenerConfig {
    @Value("${spring.kafka.bootstrap-servers:9092}")
    private String bootstrapServers;
    private final KafkaTopicsConfig topicsConfig;
    @Qualifier("dlqKafkaTemplate")
    private final KafkaTemplate<UUID, Object> dlqKafkaTemplate;

    @Bean
    public ConsumerFactory<UUID, TransactionCreatedEventModel> transactionCreatedEventModelConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, UUIDDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new org.apache.kafka.common.serialization.UUIDDeserializer(),
                new JacksonJsonDeserializer<>()
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<UUID, TransactionCreatedEventModel> transactionCreatedEventModelConcurrentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, TransactionCreatedEventModel> factory = new ConcurrentKafkaListenerContainerFactory<>();
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
                    log.error("Не удалось обработать сообщения из топика {} : {}", consumerRecord.topic(), e.getMessage());

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
