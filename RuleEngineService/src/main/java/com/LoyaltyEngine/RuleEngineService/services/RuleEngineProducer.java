package com.LoyaltyEngine.RuleEngineService.services;

import com.LoyaltyEngine.RuleEngineService.models.eventModels.CalculatedCashbackEventModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RuleEngineProducer {

    private final KafkaTemplate<UUID, CalculatedCashbackEventModel> kafkaTemplate;

    public RuleEngineProducer(
            @Qualifier(value = "calculatedCashbackEventModelKafkaTemplate")
            KafkaTemplate<UUID, CalculatedCashbackEventModel> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendCalculatedCashback(UUID transactionId, CalculatedCashbackEventModel calculatedCashback) {
        kafkaTemplate.send("points.calculated", transactionId, calculatedCashback);
    }
}
