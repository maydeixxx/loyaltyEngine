package com.LoyaltyEngine.UserService.services.configs;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Configuration
public class UserConsumerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private final KafkaTemplate<Object, Object> dlqKafkaTemplate;

    public UserConsumerConfig(@Qualifier("dlqKafkaTemplate") KafkaTemplate<Object, Object> dlqKafkaTemplate) {
        this.dlqKafkaTemplate = dlqKafkaTemplate;
    }

    @Bean
    public ConsumerFactory<UUID, Long> longConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, UUIDDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "*");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new UUIDDeserializer(),
                new JacksonJsonDeserializer<>()
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<UUID, Long> longConcurrentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, Long> containerFactory = new ConcurrentKafkaListenerContainerFactory<>();
        containerFactory.setConsumerFactory(longConsumerFactory());
        containerFactory.setCommonErrorHandler(errorHandler());
        return containerFactory;
    }

    @Bean
    public DefaultErrorHandler errorHandler() {
        ExponentialBackOffWithMaxRetries backoff = new ExponentialBackOffWithMaxRetries(3);
        backoff.setInitialInterval(1000);
        backoff.setMultiplier(2);
        backoff.setMaxInterval(4000);

        return new DefaultErrorHandler(
                (record, ex) -> {
                    log.error("Error sending message in topic {} : {}", record.topic(), ex.getMessage());
                    dlqKafkaTemplate.send(record.topic() + ".DLQ", record.key(), record.value());
                },
                backoff
        );
    }

}
