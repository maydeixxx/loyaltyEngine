package com.LoyaltyEngine.TransactionService.services;

import com.LoyaltyEngine.TransactionService.exceptions.TransactionNotFoundException;
import com.LoyaltyEngine.TransactionService.exceptions.TransactionRepositoryException;
import com.LoyaltyEngine.TransactionService.models.domain.TransactionDomain;
import com.LoyaltyEngine.TransactionService.models.domain.Status;
import com.LoyaltyEngine.TransactionService.models.entity.Transaction;
import com.LoyaltyEngine.TransactionService.models.eventModels.TransactionCreated;
import com.LoyaltyEngine.TransactionService.services.interfaces.TransactionMapper;
import com.LoyaltyEngine.TransactionService.services.interfaces.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.DataException;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final KafkaTemplate<UUID, TransactionCreated> transactionCreatedKafkaTemplate;

    public TransactionDomain createTransaction(Long userId, BigDecimal amount, UUID idempotencyKey) {
        TransactionDomain newTransaction = TransactionDomain.builder()
                .createdAt(LocalDateTime.now())
                .idempotencyKey(idempotencyKey)
                .userId(userId)
                .amount(amount)
                .status(Status.NEW)
                .build();

        TransactionCreated transactionCreated = TransactionCreated.builder()
                .userId(userId)
                .amount(amount)
                .createdAt(newTransaction.getCreatedAt())
                .build();

        try {
            Transaction savedTransaction = transactionRepository.save(transactionMapper.transactionDomainToEntity(newTransaction));
            transactionCreatedKafkaTemplate.send("transaction_created", savedTransaction.getId(), transactionCreated);
            return transactionMapper.transactionEntityToDomain(savedTransaction);
        } catch (DataException e) {
            throw new TransactionRepositoryException("Ошибка при сохранении транзакции");
        } catch (KafkaException e) {
            throw new KafkaException("Ошибка при отправке новой транзакции в топик transaction_created: " + e.getMessage());
        }
    }

    public TransactionDomain getTransactionById(UUID id) {
        return transactionMapper
                .transactionEntityToDomain(transactionRepository
                        .getTransactionById(id)
                        .orElseThrow(() -> new TransactionNotFoundException(String.format("Transaction %s not found", id)))
                );
    }

    public Optional<Transaction> getTransactionByIdempotencyKey(UUID idempotencyKey) {
        return transactionRepository.getTransactionByIdempotencyKey(idempotencyKey);
    }
}
