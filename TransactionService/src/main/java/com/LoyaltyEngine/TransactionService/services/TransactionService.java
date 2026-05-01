package com.LoyaltyEngine.TransactionService.services;

import com.LoyaltyEngine.TransactionService.exceptions.TransactionNotFoundException;
import com.LoyaltyEngine.TransactionService.models.domain.TransactionDomain;
import com.LoyaltyEngine.TransactionService.models.entity.Status;
import com.LoyaltyEngine.TransactionService.services.interfaces.TransactionMapper;
import com.LoyaltyEngine.TransactionService.services.interfaces.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    public void createTransaction(Long userId, BigDecimal amount) {
        TransactionDomain newTransaction = TransactionDomain.builder()
                .createdAt(LocalDateTime.now())
                .userId(userId)
                .amount(amount)
                .status(Status.NEW)
                .build();

        transactionRepository.save(transactionMapper.transactionDomainToEntity(newTransaction));
    }

    public TransactionDomain getTransactionById(UUID id) {
        return transactionMapper
                .transactionEntityToDomain(transactionRepository
                        .getTransactionById(id)
                        .orElseThrow(() -> new TransactionNotFoundException(String.format("Transaction %s not found", id))));
    }
}
