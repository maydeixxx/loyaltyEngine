package com.LoyaltyEngine.RuleEngineService.models;

import com.LoyaltyEngine.RuleEngineService.exceptions.CashbackRuleValidationException;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CashbackRuleDomain {
    @Setter
    private UUID id;
    private final String category;
    private final BigDecimal percentage;
    private final LocalDateTime validFrom;
    private final LocalDateTime validTo;
    private final boolean active;

    public static CashbackRuleDomain createCashbackRule(String category, BigDecimal percentage, LocalDateTime validFrom, LocalDateTime validTo, boolean active, LocalDateTime now) {
        if (category == null || category.isBlank()) {
            throw new CashbackRuleValidationException("Category cannot be null or blank");
        }

        if (percentage.compareTo(BigDecimal.ONE) < 0) {
            throw new CashbackRuleValidationException("Percentage cannot be less than 1");
        }

        if (validFrom == null || validFrom.isBefore(now)) {
            throw new CashbackRuleValidationException("Valid from cannot be null or before present time");
        }

        if (validTo == null || validTo.isBefore(now)) {
            throw new CashbackRuleValidationException("Valid to cannot be null or before present time");
        }

        if (validFrom.isAfter(validTo)) {
            throw new CashbackRuleValidationException("Valid from cannot be after validTo");
        }

        return new CashbackRuleDomain(category, percentage, validFrom, validTo, active);
    }
}
