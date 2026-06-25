package com.LoyaltyEngine.WalletService.models.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalculatedCashbackEventModel {
    private UUID transactionId;
    private Long userId;
    private BigDecimal amount;
}
