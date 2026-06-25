package com.LoyaltyEngine.RuleEngineService.services.configs;

import com.LoyaltyEngine.RuleEngineService.models.eventModels.CalculatedCashbackEventModel;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.UUIDSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
public class KafkaProducerConfig {
    @Value("${spring.kafka.bootstrap-servers:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<UUID, CalculatedCashbackEventModel> calculatedCashbackEventModelProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, UUIDSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 15000);
        props.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<UUID, CalculatedCashbackEventModel> calculatedCashbackEventModelKafkaTemplate() {
        return new KafkaTemplate<>(calculatedCashbackEventModelProducerFactory());
    }

    @Bean
    public ProducerFactory<UUID, Object> dlqProducerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 15000);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, UUIDSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<UUID, Object> dlqKafkaTemplate() {
        return new KafkaTemplate<>(dlqProducerFactory());
    }
}
