package com.LoyaltyEngine.RuleEngineService.services;

import com.LoyaltyEngine.RuleEngineService.models.eventModels.TransactionCreatedEvent;
import com.LoyaltyEngine.RuleEngineService.models.eventModels.TransactionItemEvent;
import com.LoyaltyEngine.RuleEngineService.services.interfaces.CashbackStrategy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DefaultCashbackStrategy implements CashbackStrategy {
    private static final BigDecimal DEFAULT_PERCENTAGE = new BigDecimal("1.00");

    @Override
    public Optional<BigDecimal> calculate(TransactionItemEvent item, TransactionCreatedEvent transaction) {
        return Optional.of(DEFAULT_PERCENTAGE);
    }
}
