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

    private final MeterRegistry registry;
    private final Tracer tracer;

    @KafkaListener(
            topics = "${kafka.topics.transaction-created}",
            groupId = "rule_engine_service",
            containerFactory = "transactionCreatedEventModelConcurrentKafkaListenerContainerFactory"
    )
    public void handleTransactionCreatedEvent(ConsumerRecord<UUID, TransactionCreatedEvent> record) {
        ScopedSpan span = tracer.startScopedSpan("calculate-cashback-span");
        Timer.Sample timer = Timer.start(registry);

        try {
            UUID transactionId = record.key();
            CalculatedCashbackEventModel calculatedCashbackModel = calculateCashbackService.calculateCashback(record.value(), transactionId);

            ruleEngineProducer.sendCalculatedCashback(transactionId, calculatedCashbackModel);
            registry.counter("calculate.cashback", "status", "successful").increment();
            log.info("Total cashback for transaction {} : {}", transactionId, calculatedCashbackModel.amount());
        } catch (Exception e) {
            registry.counter("calculate.cashback", "status", "failed").increment();
            log.error("Error handling transaction created event: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            span.end();
            timer.stop(registry.timer("calculate.cashback.duration"));
        }

    }

}
