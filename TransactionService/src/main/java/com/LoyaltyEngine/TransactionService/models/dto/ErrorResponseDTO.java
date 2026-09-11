package com.LoyaltyEngine.TransactionService.models.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
@Value
public class ErrorResponseDTO {
    String error;
    Map<String, String> errors;
    String message;
    int status;
    String path;
    LocalDateTime timestamp;
}
