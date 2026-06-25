package com.LoyaltyEngine.WalletService.models.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionHandledEvent {
    private UUID transactionId;
    private Long userId;
}
