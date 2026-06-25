package com.LoyaltyEngine.RuleEngineService.models.eventModels;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionCreatedEvent {
    private UUID transactionId;
    private Long userId;
    private BigDecimal amount;
    private List<TransactionItemEvent> items;
    private LocalDateTime createdAt;
}
