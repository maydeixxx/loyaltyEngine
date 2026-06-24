package com.LoyaltyEngine.RuleEngineService.services;

import com.LoyaltyEngine.RuleEngineService.models.eventModels.CalculatedCashbackEventModel;
import com.LoyaltyEngine.RuleEngineService.services.configs.KafkaTopicsConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RuleEngineProducer {
    private final KafkaTopicsConfig topicsConfig;
    private final KafkaTemplate<UUID, CalculatedCashbackEventModel> kafkaTemplate;

    public RuleEngineProducer(
            @Qualifier(value = "calculatedCashbackEventModelKafkaTemplate")
            KafkaTemplate<UUID, CalculatedCashbackEventModel> kafkaTemplate,
            KafkaTopicsConfig topicsConfig
    ) {
        this.topicsConfig = topicsConfig;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendCalculatedCashback(UUID transactionId, CalculatedCashbackEventModel calculatedCashback) {
        kafkaTemplate.send(topicsConfig.getPointsCalculated(), transactionId, calculatedCashback);
    }
}
