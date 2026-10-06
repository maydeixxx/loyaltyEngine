package com.LoyaltyEngine.RuleEngineService.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cashback_rules", indexes = {
        @Index(name = "idx_cashback_rules_category_dates", columnList = "category, validFrom, validTo")
})
public class CashbackRule {
    @Id
    private UUID id;

    @Column(unique = true, nullable = false)
    private String category;

    @Column(nullable = false)
    private BigDecimal percentage;

    @Column(nullable = false)
    private LocalDateTime validFrom;
    @Column(nullable = false)
    private LocalDateTime validTo;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CashbackRule that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
