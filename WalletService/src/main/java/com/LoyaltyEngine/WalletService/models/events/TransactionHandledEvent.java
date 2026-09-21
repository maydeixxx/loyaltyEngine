package com.LoyaltyEngine.WalletService.models.events;


import java.util.UUID;

public record TransactionHandledEvent(
        UUID transactionId,
        UUID userId
) {}
