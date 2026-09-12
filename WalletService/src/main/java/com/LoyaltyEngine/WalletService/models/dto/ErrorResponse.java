package com.LoyaltyEngine.WalletService.models.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ErrorResponse(
        int code,
        String error,
        String message,
        String path,
        LocalDateTime timestamp
) {}
