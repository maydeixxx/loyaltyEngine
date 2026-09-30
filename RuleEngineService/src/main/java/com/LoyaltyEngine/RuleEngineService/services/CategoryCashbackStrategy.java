package com.LoyaltyEngine.RuleEngineService.services;

import com.LoyaltyEngine.RuleEngineService.models.eventModels.TransactionCreatedEvent;
import com.LoyaltyEngine.RuleEngineService.models.eventModels.TransactionItemEvent;
import com.LoyaltyEngine.RuleEngineService.services.interfaces.CashbackStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Order(1)
public class CategoryCashbackStrategy implements CashbackStrategy {
    private final RuleEngineService ruleEngineService;

    @Override
    public Optional<BigDecimal> calculate(TransactionItemEvent item, TransactionCreatedEvent transaction) {
        if (item.category() == null || item.category().isBlank()) return Optional.empty();
        return ruleEngineService.getPercentageForCategory(item.category());
    }
}
