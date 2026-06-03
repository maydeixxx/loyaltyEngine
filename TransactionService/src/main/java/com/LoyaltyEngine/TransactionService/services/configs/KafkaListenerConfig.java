package com.LoyaltyEngine.TransactionService.services.configs;

import com.LoyaltyEngine.TransactionService.models.eventModels.TransactionCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import tools.jackson.databind.deser.jdk.UUIDDeserializer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
public class KafkaListenerConfig {
    @Value("${spring.kafka.bootstrap-servers:9092}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<UUID, TransactionCreatedEvent> transactionCreatedEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        try (JacksonJsonDeserializer<TransactionCreatedEvent> deserializer = new JacksonJsonDeserializer<>(TransactionCreatedEvent.class)) {
            deserializer.addTrustedPackages("*");
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, UUIDDeserializer.class);
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializer);

            return new DefaultKafkaConsumerFactory<>(
                    props,
                    new org.apache.kafka.common.serialization.UUIDDeserializer(),
                    deserializer
            );
        }
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<UUID, TransactionCreatedEvent> transactionCreatedEventConcurrentKafkaListenerContainerFactory(ConsumerFactory<UUID, TransactionCreatedEvent> transactionCreatedEventConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<UUID, TransactionCreatedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(transactionCreatedEventConsumerFactory);

        return factory;
    }
}
