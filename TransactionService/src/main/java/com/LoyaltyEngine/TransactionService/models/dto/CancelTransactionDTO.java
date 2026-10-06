package com.LoyaltyEngine.TransactionService.models.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CancelTransactionDTO(
        @NotNull(message = "Transaction id cant be null")
        UUID transactionId,

        @NotNull(message = "User id cant be null")
        UUID userId
) {
}
