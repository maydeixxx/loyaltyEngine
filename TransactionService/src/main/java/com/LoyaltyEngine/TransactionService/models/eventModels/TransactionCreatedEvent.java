package com.LoyaltyEngine.TransactionService.models.eventModels;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TransactionCreatedEvent(
        UUID transactionId,
        UUID userId,
        BigDecimal amount,
        List<TransactionItemEvent> items,
        LocalDateTime createdAt,
        Boolean useCashbackBalance
) {
}
