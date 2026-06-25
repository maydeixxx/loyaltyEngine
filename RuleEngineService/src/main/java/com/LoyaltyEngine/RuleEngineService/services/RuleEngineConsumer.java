package com.LoyaltyEngine.RuleEngineService.services;

import com.LoyaltyEngine.RuleEngineService.models.eventModels.CalculatedCashbackEventModel;
import com.LoyaltyEngine.RuleEngineService.models.eventModels.TransactionCreatedEvent;
import com.LoyaltyEngine.RuleEngineService.models.eventModels.TransactionItemEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleEngineConsumer {
    private final RuleEngineService ruleEngineService;
    private final RuleEngineProducer ruleEngineProducer;
    private final BigDecimal hundred = new BigDecimal("100.0");

    @KafkaListener(
            topics = "${kafka.topics.transaction-created}",
            groupId = "rule_engine_service",
            containerFactory = "transactionCreatedEventModelConcurrentKafkaListenerContainerFactory"
    )
    private void handleTransactionCreatedEvent(ConsumerRecord<UUID, TransactionCreatedEvent> record) {
        TransactionCreatedEvent model = record.value();

        UUID transactionId = record.key();
        Long userId = model.getUserId();
        BigDecimal cashback = BigDecimal.ZERO;

        for (TransactionItemEvent item : model.getItems()) {
            BigDecimal itemPrice = item.getPrice();
            BigDecimal percentageForCategory = ruleEngineService.getPercentageForCategory(item.getCategory());

            cashback = cashback.add(itemPrice.multiply(percentageForCategory).divide(hundred, 2, RoundingMode.HALF_UP));
        }

        CalculatedCashbackEventModel calculatedCashbackModel = CalculatedCashbackEventModel.builder()
                .transactionId(transactionId)
                .userId(userId)
                .amount(cashback)
                .build();

        ruleEngineProducer.sendCalculatedCashback(transactionId, calculatedCashbackModel);
        log.info("Total cashback for transaction {} : {}", transactionId, cashback);
    }

}
