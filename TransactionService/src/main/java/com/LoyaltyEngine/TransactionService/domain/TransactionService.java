package com.LoyaltyEngine.TransactionService.domain;

import com.LoyaltyEngine.TransactionService.entity.Status;
import com.LoyaltyEngine.TransactionService.entity.interfaces.TransactionMapper;
import com.LoyaltyEngine.TransactionService.entity.interfaces.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
}
