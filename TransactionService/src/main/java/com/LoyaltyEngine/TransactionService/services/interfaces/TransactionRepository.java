package com.LoyaltyEngine.TransactionService.services.interfaces;


import com.LoyaltyEngine.TransactionService.models.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> getTransactionById(UUID id);
    Optional<Transaction> getTransactionByIdempotencyKey(UUID idempotencyKey);
    List<Transaction> getTransactionsByUserId(Long userId);
}
