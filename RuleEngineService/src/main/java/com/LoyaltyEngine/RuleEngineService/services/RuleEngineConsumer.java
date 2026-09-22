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
    public void handleTransactionCreatedEvent(ConsumerRecord<UUID, TransactionCreatedEvent> record) {
        try {
            TransactionCreatedEvent model = record.value();

            UUID transactionId = record.key();
            UUID userId = model.userId();
            BigDecimal amountOfTransaction = model.amount();
            BigDecimal cashback = BigDecimal.ZERO;
            BigDecimal totalItemPrice = BigDecimal.ZERO;

            for (TransactionItemEvent item : model.items()) {
                BigDecimal itemPrice = item.price();
                String category = item.category();

                BigDecimal percentageForCategory = new BigDecimal("1.00");
                if (category != null) {
                    percentageForCategory = ruleEngineService.getPercentageForCategory(category);
                }

                totalItemPrice = totalItemPrice.add(itemPrice);
                cashback = cashback.add(itemPrice.multiply(percentageForCategory).divide(hundred, 2, RoundingMode.HALF_EVEN));
            }

            CalculatedCashbackEventModel calculatedCashbackModel = new CalculatedCashbackEventModel(
                    transactionId,
                    userId,
                    amountOfTransaction,
                    totalItemPrice,
                    cashback,
                    model.useCashbackBalance()
            );

            ruleEngineProducer.sendCalculatedCashback(transactionId, calculatedCashbackModel);
            log.info("Total cashback for transaction {} : {}", transactionId, cashback);
        } catch (Exception e) {
            log.error("Error handling transaction created event: {}", e.getMessage());
            throw new RuntimeException(e);
        }

    }

}
