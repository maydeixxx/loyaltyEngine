package com.LoyaltyEngine.RuleEngineService.services;

import com.LoyaltyEngine.RuleEngineService.models.eventModels.CalculatedCashbackEventModel;
import com.LoyaltyEngine.RuleEngineService.models.eventModels.TransactionCreatedEvent;
import com.LoyaltyEngine.RuleEngineService.models.eventModels.TransactionItemEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
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
            registry.counter("calculate.cashback", "status", "successful").increment();
            log.info("Total cashback for transaction {} : {}", transactionId, cashback);
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
