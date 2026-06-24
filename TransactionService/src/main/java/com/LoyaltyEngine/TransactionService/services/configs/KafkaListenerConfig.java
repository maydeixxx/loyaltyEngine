package com.LoyaltyEngine.TransactionService.services.configs;

import com.LoyaltyEngine.TransactionService.models.eventModels.PointsFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
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
@RequiredArgsConstructor
public class KafkaListenerConfig {
    @Value("${spring.kafka.bootstrap-servers:9092}")
    private String bootstrapServers;
    @Qualifier("dlqKafkaTemplate")
    private final KafkaTemplate<UUID, String> dlqKafkaTemplate;
    @Value("${kafka.topics.dlqSuffix}")
    private String dlqSuffix;

    @Bean
    public ConsumerFactory<UUID, PointsFailedEvent> pointsFailedEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, UUIDDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new UUIDDeserializer(),
                new JacksonJsonDeserializer<>()
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<UUID, PointsFailedEvent> pointsFailedEventConcurrentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, PointsFailedEvent> containerFactory = new ConcurrentKafkaListenerContainerFactory<>();
        containerFactory.setConsumerFactory(pointsFailedEventConsumerFactory());
        containerFactory.setCommonErrorHandler(errorHandler());

        return containerFactory;
    }

    @Bean
    public DefaultErrorHandler errorHandler() {
        ExponentialBackOffWithMaxRetries exponentialBackOffWithMaxRetries = new ExponentialBackOffWithMaxRetries(3);
        exponentialBackOffWithMaxRetries.setInitialInterval(1000L);
        exponentialBackOffWithMaxRetries.setMultiplier(2);
        exponentialBackOffWithMaxRetries.setMaxInterval(4000L);

        return new DefaultErrorHandler(
                (consumer, ex) -> {
                    log.error("Ошибка при отправке сообщения в топик {} : {}", consumer.topic(), ex.getMessage());

                    dlqKafkaTemplate.send(consumer.topic() + dlqSuffix, (UUID) consumer.key(), (String) consumer.value());

                    log.info("Отправлено сообщение в DLQ topic: {}", consumer.topic());
                }
        );
    }

}
