package com.LoyaltyEngine.TransactionService.models.dto;

import com.LoyaltyEngine.TransactionService.models.domain.Status;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class TransactionDTO {
    private UUID id;
    private Long userId;
    private UUID idempotencyKey;
    private BigDecimal amount;
    private List<TransactionItemDTO> items;
    private LocalDateTime createdAt;
    private Status status;
}
