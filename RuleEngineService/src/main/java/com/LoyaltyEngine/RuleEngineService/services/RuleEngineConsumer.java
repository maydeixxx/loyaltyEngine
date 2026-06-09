package com.LoyaltyEngine.RuleEngineService.services;

import com.LoyaltyEngine.RuleEngineService.models.eventModels.CalculatedCashbackEventModel;
import com.LoyaltyEngine.RuleEngineService.models.eventModels.TransactionCreatedEventModel;
import com.LoyaltyEngine.RuleEngineService.models.eventModels.TransactionItemEventModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
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

    @KafkaListener(topicPartitions = @TopicPartition(partitions = "{0}",
            topic = "transaction_created"),
            groupId = "ruleEngineService",
            containerFactory = "transactionCreatedEventModelConcurrentKafkaListenerContainerFactory")
    private void handleTransactionCreatedEvent(ConsumerRecord<UUID, TransactionCreatedEventModel> record) {
        TransactionCreatedEventModel model = record.value();

        UUID transactionId = record.key();
        Long userId = model.getUserId();
        BigDecimal cashback = BigDecimal.ZERO;

        for (TransactionItemEventModel item : model.getItems()) {
            BigDecimal itemPrice = item.getPrice();
            BigDecimal percentageForCategory = ruleEngineService.getPercentageForCategory(item.getCategory());

            cashback = cashback.add(itemPrice.multiply(percentageForCategory).divide(hundred, 2, RoundingMode.HALF_UP));
        }

        CalculatedCashbackEventModel calculatedCashbackModel = CalculatedCashbackEventModel.builder()
                .userId(userId)
                .amount(cashback)
                .build();

        ruleEngineProducer.sendCalculatedCashback(transactionId, calculatedCashbackModel);
        log.info("Total cashback for transaction {} : {}", transactionId, cashback);
    }

}
