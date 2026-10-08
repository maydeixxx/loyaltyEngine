package com.LoyaltyEngine.TransactionService.services.interfaces;


import com.LoyaltyEngine.TransactionService.models.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> getTransactionById(UUID id);
    Optional<Transaction> getTransactionByIdempotencyKey(UUID idempotencyKey);

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId ORDER BY t.createdAt DESC")
    List<Transaction> getTransactionsByUserId(@Param("userId") UUID userId);
}
