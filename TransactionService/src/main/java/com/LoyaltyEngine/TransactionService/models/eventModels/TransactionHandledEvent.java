package com.LoyaltyEngine.TransactionService.models.eventModels;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TransactionHandledEvent {
    private UUID transactionId;
    private Long userId;
}
