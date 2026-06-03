package com.LoyaltyEngine.RuleEngineService.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "cashbackRule")
public class CashbackRule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String category;
    private BigDecimal percentage;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    boolean active;
}
