package com.LoyaltyEngine.RuleEngineService.services;

import com.LoyaltyEngine.RuleEngineService.models.eventModels.CalculatedCashbackEventModel;
import com.LoyaltyEngine.RuleEngineService.models.eventModels.TransactionCreatedEvent;
import com.LoyaltyEngine.RuleEngineService.models.eventModels.TransactionItemEvent;
import com.LoyaltyEngine.RuleEngineService.services.interfaces.CashbackStrategy;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CalculateCashbackService {
    private final BigDecimal hundred = new BigDecimal("100.0");
    private final List<CashbackStrategy> cashbackStrategies;
    private final MeterRegistry registry;
    private final Tracer tracer;

    public CalculatedCashbackEventModel calculateCashback(TransactionCreatedEvent model, UUID transactionId) {
        ScopedSpan span = tracer.startScopedSpan("calculate-cashback-span");
        Timer.Sample timer = Timer.start(registry);

        try {
            UUID userId = model.userId();
            BigDecimal amountOfTransaction = model.amount();
            BigDecimal cashback = BigDecimal.ZERO;
            BigDecimal totalItemPrice = BigDecimal.ZERO;

            List<TransactionItemEvent> items = model.items() != null ? model.items() : List.of();
            for (TransactionItemEvent item : items) {
                BigDecimal itemPrice = item.price();

                BigDecimal percentage = cashbackStrategies.stream()
                        .map(s -> s.calculate(item, model))
                        .flatMap(Optional::stream)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("No applicable strategy for item [%s]".formatted(item.name())));

                totalItemPrice = totalItemPrice.add(itemPrice);
                cashback = cashback.add(itemPrice.multiply(percentage).divide(hundred, 2, RoundingMode.HALF_EVEN));
            }

            registry.counter("calculate.cashback", "status", "successful").increment();
            return new CalculatedCashbackEventModel(
                    transactionId,
                    userId,
                    amountOfTransaction,
                    totalItemPrice,
                    cashback,
                    model.useCashbackBalance()
            );
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error calculating cashback: {}", e.getMessage());
            registry.counter("calculate.cashback", "status", "failed").increment();
            throw new RuntimeException(e);
        } finally {
            span.end();
            timer.stop(registry.timer("calculate.cashback.duration"));
        }
    }

}
