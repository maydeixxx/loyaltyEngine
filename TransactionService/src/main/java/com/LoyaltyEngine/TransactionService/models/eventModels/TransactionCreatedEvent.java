package com.LoyaltyEngine.TransactionService.models.eventModels;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionCreatedEvent {
    private UUID transactionId;
    private Long userId;
    private BigDecimal amount;
    private List<TransactionItemEvent> items;
    private LocalDateTime createdAt;
    private Boolean useCashbackBalance;
}
