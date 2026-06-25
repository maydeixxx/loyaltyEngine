package com.LoyaltyEngine.WalletService.models.dto;

import com.LoyaltyEngine.WalletService.models.domain.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class WalletTransactionDto {
    private UUID id;
    private UUID walletId;
    private UUID transactionId;
    private BigDecimal amount;
    private TransactionType type;
    private LocalDateTime createdAt;
    private String description;
}
