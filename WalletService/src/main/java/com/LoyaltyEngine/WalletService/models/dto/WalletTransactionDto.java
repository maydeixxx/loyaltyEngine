package com.LoyaltyEngine.WalletService.models.dto;

import com.LoyaltyEngine.WalletService.models.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletTransactionDto(
        UUID id,
        UUID walletId,
        UUID transactionId,
        BigDecimal amount,
        TransactionType type,
        LocalDateTime createdAt,
        String description
) {}
