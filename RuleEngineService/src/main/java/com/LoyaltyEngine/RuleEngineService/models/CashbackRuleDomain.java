package com.LoyaltyEngine.RuleEngineService.models;

import com.LoyaltyEngine.RuleEngineService.exceptions.CashbackRuleValidationException;
import com.LoyaltyEngine.RuleEngineService.models.valueObjects.RuleId;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
public class CashbackRuleDomain {
    private final RuleId id;
    private String category;
    private BigDecimal percentage;
    private final LocalDateTime validFrom;
    private final LocalDateTime validTo;

    private CashbackRuleDomain(RuleId id, String category, BigDecimal percentage, LocalDateTime validFrom, LocalDateTime validTo) {
        LocalDateTime now = LocalDateTime.now();

        if (category == null || category.isBlank()) {
            throw new CashbackRuleValidationException("Category cannot be null or blank");
        }

        if (percentage.compareTo(BigDecimal.ONE) < 0) {
            throw new CashbackRuleValidationException("Percentage cannot be less than 1");
        }

        if (validFrom == null) {
            throw new CashbackRuleValidationException("Valid from cannot be null");
        }

        if (validTo == null) {
            throw new CashbackRuleValidationException("Valid to cannot be null");
        }

        if (validFrom.isAfter(validTo)) {
            throw new CashbackRuleValidationException("Valid from cannot be after validTo");
        }

        if (validTo.isBefore(now)) {
            throw new CashbackRuleValidationException("Valid to cant be before present time");
        }

        this.id = id;
        this.category = category;
        this.percentage = percentage;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public static CashbackRuleDomain createCashbackRule(String category, BigDecimal percentage, LocalDateTime validFrom, LocalDateTime validTo) {
        RuleId ruleId = RuleId.generateId();
        return new CashbackRuleDomain(ruleId, category, percentage, validFrom, validTo);
    }

    public static CashbackRuleDomain restoreFromExisting(UUID ruleId, String category, BigDecimal percentage, LocalDateTime validFrom, LocalDateTime validTo) {
        return new CashbackRuleDomain(new RuleId(ruleId), category, percentage, validFrom, validTo);
    }

    public void updateCategory(String category) {
        if (this.category.equals(category) || category.isBlank()) throw new IllegalArgumentException("Cant change category");
        this.category = category;
    }

    public void updatePercentage(BigDecimal percentage) {
        if (this.percentage.equals(percentage) || percentage.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Cant change percentage");
        this.percentage = percentage;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CashbackRuleDomain that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
