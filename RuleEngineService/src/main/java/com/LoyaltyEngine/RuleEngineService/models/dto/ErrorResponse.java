package com.LoyaltyEngine.RuleEngineService.models.dto;


import java.time.LocalDateTime;


public record ErrorResponse(
        String error,
        String message,
        int code,
        String path,
        LocalDateTime timestamp
) {
}
