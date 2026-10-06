package com.LoyaltyEngine.RuleEngineService.services.interfaces;

import com.LoyaltyEngine.RuleEngineService.models.eventModels.TransactionCreatedEvent;
import com.LoyaltyEngine.RuleEngineService.models.eventModels.TransactionItemEvent;

import java.math.BigDecimal;
import java.util.Optional;

public interface CashbackStrategy {
    Optional<BigDecimal> calculate(TransactionItemEvent item, TransactionCreatedEvent transaction);
}
