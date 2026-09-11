package com.LoyaltyEngine.TransactionService.models.dto;

import java.time.LocalDateTime;
import java.util.Map;


public record ErrorResponseDTO (
    String error,
    Map<String, String> errors,
    String message,
    int status,
    String path,
    LocalDateTime timestamp
) {}
