package com.LoyaltyEngine.TransactionService.services;

import com.LoyaltyEngine.TransactionService.exceptions.TransactionKafkaException;
import com.LoyaltyEngine.TransactionService.exceptions.TransactionNotFoundException;
import com.LoyaltyEngine.TransactionService.exceptions.TransactionRepositoryException;
import com.LoyaltyEngine.TransactionService.models.domain.TransactionDomain;
import com.LoyaltyEngine.TransactionService.models.domain.TransactionItemDomain;
import com.LoyaltyEngine.TransactionService.models.entity.OutboxEvent;
import com.LoyaltyEngine.TransactionService.models.entity.Transaction;
import com.LoyaltyEngine.TransactionService.models.eventModels.TransactionCreatedEvent;
import com.LoyaltyEngine.TransactionService.models.eventModels.TransactionItemEvent;
import com.LoyaltyEngine.TransactionService.services.interfaces.OutboxEventRepository;
import com.LoyaltyEngine.TransactionService.services.interfaces.TransactionMapper;
import com.LoyaltyEngine.TransactionService.services.interfaces.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.DataException;
import org.springframework.kafka.KafkaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final ObjectMapper mapper;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public TransactionDomain createTransaction(Long userId, BigDecimal amount, List<TransactionItemDomain> items, UUID idempotencyKey) {
        Optional<TransactionDomain> transactionByIdempotencyKey = getTransactionByIdempotencyKey(idempotencyKey);
        if (transactionByIdempotencyKey.isPresent()) {
            return transactionByIdempotencyKey.get();
        }

        TransactionDomain newTransaction = TransactionDomain.create(userId, amount, idempotencyKey, items);

        List<TransactionItemEvent> eventItems = items
                .stream()
                .map(
                        item -> TransactionItemEvent.builder()
                                .name(item.getName())
                                .price(item.getPrice())
                                .category(item.getCategory())
                                .build()
                )
                .toList();

        TransactionCreatedEvent transactionCreated = TransactionCreatedEvent.builder()
                .userId(userId)
                .amount(amount)
                .createdAt(newTransaction.getCreatedAt())
                .items(eventItems)
                .build();

        try {
            Transaction savedTransaction = transactionRepository.save(transactionMapper.transactionDomainToEntity(newTransaction));
            transactionCreated.setTransactionId(savedTransaction.getId());

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(savedTransaction.getId())
                    .createdAt(LocalDateTime.now())
                    .eventType("transaction.created")
                    .payload(mapper.writeValueAsString(transactionCreated))
                    .processed(false)
                    .build();
            outboxEventRepository.save(outboxEvent);

            return transactionMapper.transactionEntityToDomain(savedTransaction);
        } catch (DataException e) {
            throw new TransactionRepositoryException("Ошибка при сохранении транзакции", e);
        } catch (KafkaException e) {
            throw new TransactionKafkaException("Ошибка при отправке новой транзакции в топик transaction_created: " + e.getMessage());
        }
    }

    public TransactionDomain getTransactionById(UUID id) {
        return transactionMapper
                .transactionEntityToDomain(transactionRepository
                        .getTransactionById(id)
                        .orElseThrow(() -> new TransactionNotFoundException(String.format("Transaction %s not found", id)))
                );
    }

    public Optional<TransactionDomain> getTransactionByIdempotencyKey(UUID idempotencyKey) {
        try {
            return transactionMapper.transactionEntityToDomain(transactionRepository.getTransactionByIdempotencyKey(idempotencyKey));
        } catch (DataException e) {
            throw new TransactionRepositoryException("Ошибка при поиске transaction по idempotencyKey", e);
        }
    }

    public List<TransactionDomain> getTransactionByUserId(Long id) {
        try {
            List<Transaction> transactions = transactionRepository.getTransactionsByUserId(id);
            return transactions
                    .stream()
                    .map(transactionMapper::transactionEntityToDomain)
                    .toList();
        } catch (DataException e) {
            throw new TransactionRepositoryException("Ошибка при получении транзакций", e);
        }
    }
}
