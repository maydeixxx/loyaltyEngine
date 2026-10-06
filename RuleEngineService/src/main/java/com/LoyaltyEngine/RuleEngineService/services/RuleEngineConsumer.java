package com.LoyaltyEngine.RuleEngineService.services;

import com.LoyaltyEngine.RuleEngineService.models.eventModels.CalculatedCashbackEventModel;
import com.LoyaltyEngine.RuleEngineService.models.eventModels.TransactionCreatedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleEngineConsumer {
    private final CalculateCashbackService calculateCashbackService;
    private final RuleEngineProducer ruleEngineProducer;

    @KafkaListener(
            topics = "${kafka.topics.transaction-created}",
            groupId = "rule_engine_service",
            containerFactory = "transactionCreatedEventModelConcurrentKafkaListenerContainerFactory"
    )
    public void handleTransactionCreatedEvent(ConsumerRecord<UUID, TransactionCreatedEvent> record) {
        UUID transactionId = record.key();
        CalculatedCashbackEventModel calculatedCashbackModel = calculateCashbackService.calculateCashback(record.value(), transactionId);

        ruleEngineProducer.sendCalculatedCashback(transactionId, calculatedCashbackModel);
        log.info("Total cashback for transaction {} : {}", transactionId, calculatedCashbackModel.amount());
    }

}
