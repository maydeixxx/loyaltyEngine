package com.LoyaltyEngine.TransactionService.models.dto;

import java.time.LocalDateTime;

public record ErrorResponseDTO (
    String error,
    String message,
    int status,
    String path,
    LocalDateTime timestamp
) {}
