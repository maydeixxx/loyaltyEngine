package com.LoyaltyEngine.TransactionService.services.configs;

import com.LoyaltyEngine.TransactionService.exceptions.TransactionNotFoundException;
import com.LoyaltyEngine.TransactionService.models.eventModels.CancellationFailedEvent;
import com.LoyaltyEngine.TransactionService.models.eventModels.PointsFailedEvent;
import com.LoyaltyEngine.TransactionService.models.eventModels.TransactionHandledEvent;
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
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Configuration
public class KafkaListenerConfig {
    @Value("${spring.kafka.bootstrap-servers:9092}")
    private String bootstrapServers;
    private final KafkaTemplate<UUID, Object> dlqKafkaTemplate;
    @Value("${kafka.topics.dlqSuffix}")
    private String dlqSuffix;

    public KafkaListenerConfig(@Qualifier("dlqKafkaTemplate") KafkaTemplate<UUID, Object> dlqKafkaTemplate) {
        this.dlqKafkaTemplate = dlqKafkaTemplate;
    }

    @Bean
    public ConsumerFactory<UUID, PointsFailedEvent> pointsFailedEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, UUIDDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "*");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new UUIDDeserializer(),
                new ErrorHandlingDeserializer<>(new JacksonJsonDeserializer<>(PointsFailedEvent.class, false))
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
    public ConsumerFactory<UUID, CancellationFailedEvent> cancellationFailedEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, UUIDDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "*");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new UUIDDeserializer(),
                new ErrorHandlingDeserializer<>(new JacksonJsonDeserializer<>(CancellationFailedEvent.class, false))
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<UUID, CancellationFailedEvent> cancellationFailedEventConcurrentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, CancellationFailedEvent> containerFactory = new ConcurrentKafkaListenerContainerFactory<>();
        containerFactory.setConsumerFactory(cancellationFailedEventConsumerFactory());
        containerFactory.setCommonErrorHandler(errorHandler());

        return containerFactory;
    }

    @Bean
    public ConsumerFactory<UUID, TransactionHandledEvent> transactionHandledEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, UUIDDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "*");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new UUIDDeserializer(),
                new ErrorHandlingDeserializer<>(new JacksonJsonDeserializer<>(TransactionHandledEvent.class, false))
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<UUID, TransactionHandledEvent> transactionHandledEventContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, TransactionHandledEvent> containerFactory = new ConcurrentKafkaListenerContainerFactory<>();
        containerFactory.setConsumerFactory(transactionHandledEventConsumerFactory());
        containerFactory.setCommonErrorHandler(errorHandler());

        return containerFactory;
    }

    @Bean
    public DefaultErrorHandler errorHandler() {
        ExponentialBackOffWithMaxRetries exponentialBackOffWithMaxRetries = new ExponentialBackOffWithMaxRetries(3);
        exponentialBackOffWithMaxRetries.setInitialInterval(1000L);
        exponentialBackOffWithMaxRetries.setMultiplier(2);
        exponentialBackOffWithMaxRetries.setMaxInterval(4000L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (consumer, ex) -> {
                    log.error("Sent new message in {} : {}", consumer.topic(), ex.getMessage());

                    dlqKafkaTemplate.send(consumer.topic() + dlqSuffix, (UUID) consumer.key(), consumer.value());

                    log.info("Sent new message in DLQ topic: {}", consumer.topic() + dlqSuffix);
                },
                exponentialBackOffWithMaxRetries
        );
        errorHandler.addNotRetryableExceptions(
                TransactionNotFoundException.class,
                IllegalArgumentException.class
        );

        return errorHandler;
    }

}
