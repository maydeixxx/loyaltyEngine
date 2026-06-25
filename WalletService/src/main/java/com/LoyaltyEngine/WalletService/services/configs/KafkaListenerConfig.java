package com.LoyaltyEngine.WalletService.services.configs;

import com.LoyaltyEngine.WalletService.models.events.CalculatedCashbackEventModel;
import lombok.RequiredArgsConstructor;
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
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Configuration
public class KafkaListenerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    private final KafkaTemplate<UUID, Object> dlqKafkaTemplate;
    @Value("${kafka.topics.dlq-suffix}")
    private String dlqSuffix;

    public KafkaListenerConfig(@Qualifier("dlqKafkaTemplate") KafkaTemplate<UUID, Object> dlqKafkaTemplate) {
        this.dlqKafkaTemplate = dlqKafkaTemplate;
    }

    @Bean
    public ConsumerFactory<UUID, CalculatedCashbackEventModel> calculatedCashbackEventModelConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, UUIDDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "*");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new UUIDDeserializer(),
                new JacksonJsonDeserializer<>(CalculatedCashbackEventModel.class, false)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<UUID, CalculatedCashbackEventModel> calculatedCashbackEventModelConcurrentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, CalculatedCashbackEventModel> containerFactory = new ConcurrentKafkaListenerContainerFactory<>();
        containerFactory.setConsumerFactory(calculatedCashbackEventModelConsumerFactory());
        containerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        containerFactory.setCommonErrorHandler(errorHandler());

        return containerFactory;
    }

    @Bean
    public DefaultErrorHandler errorHandler() {
        ExponentialBackOffWithMaxRetries backOffWithMaxRetries = new ExponentialBackOffWithMaxRetries(3);
        backOffWithMaxRetries.setMaxInterval(4000L);
        backOffWithMaxRetries.setInitialInterval(1000L);
        backOffWithMaxRetries.setMultiplier(2);

        return new DefaultErrorHandler(
                (record, ex) -> {
                    log.error("Error handling message from topic {} : {}", record.topic(), ex.getMessage());

                    dlqKafkaTemplate.send(record.topic() + dlqSuffix, (UUID) record.key(), record.value());
                },
                backOffWithMaxRetries
        );
    }
}
