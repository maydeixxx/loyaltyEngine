package com.LoyaltyEngine.WalletService.models.events;


import java.math.BigDecimal;
import java.util.UUID;

public record CalculatedCashbackEventModel(
        UUID transactionId,
        UUID userId,
        BigDecimal amountOfTransaction,
        BigDecimal totalItemPrice,
        BigDecimal amount,
        Boolean useCashback
) {}
