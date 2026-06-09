package com.LoyaltyEngine.RuleEngineService.services;

import com.LoyaltyEngine.RuleEngineService.models.CashbackRule;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RuleEngineService {
    private final RuleEngineRepository ruleEngineRepository;
    private final BigDecimal basePercentage = new BigDecimal("1.0");

    @Cacheable(value = "cashback_rules", key = "#category")
    public BigDecimal getPercentageForCategory(String category) {
        Optional<CashbackRule> cashbackRuleByCategory = ruleEngineRepository.getCashbackRuleByCategory(category);

        if (cashbackRuleByCategory.isPresent()) {
            return cashbackRuleByCategory.get().getPercentage();
        } else {
            return basePercentage;
        }
    }
}
