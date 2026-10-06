package com.LoyaltyEngine.RuleEngineService.models;

import com.LoyaltyEngine.RuleEngineService.exceptions.CashbackRuleValidationException;
import com.LoyaltyEngine.RuleEngineService.exceptions.CashbackUpdateException;
import com.LoyaltyEngine.RuleEngineService.models.valueObjects.RuleId;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
public class CashbackRuleDomain {
    private final RuleId id;
    private final String category;
    private BigDecimal percentage;
    private final LocalDateTime validFrom;
    private LocalDateTime validTo;

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
        this.category = category.toLowerCase().trim();
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

    public void updatePercentage(BigDecimal percentage) {
        if (percentage == null || this.percentage.compareTo(percentage) == 0 || percentage.compareTo(BigDecimal.ONE) < 0) throw new CashbackUpdateException("Entered not valid new percentage");
        this.percentage = percentage;
    }

    public void updateValidTo(LocalDateTime newValidTo) {
        if (newValidTo == null) throw new CashbackUpdateException("New valid to cant be null");
        if (newValidTo.isBefore(this.validFrom) || newValidTo.isBefore(LocalDateTime.now())) throw new CashbackUpdateException("New valid to is not valid");
        this.validTo = newValidTo;
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
