package com.LoyaltyEngine.TransactionService.models.apiResponses;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ErrorResponse {
    private final String error;
    private final Map<String, String> errors;
    private final String message;
    private final int status;
    private final String path;
    private final LocalDateTime timestamp;
}
