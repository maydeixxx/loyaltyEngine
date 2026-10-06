package com.LoyaltyEngine.TransactionService.services;

import com.LoyaltyEngine.TransactionService.exceptions.TransactionMappingException;
import com.LoyaltyEngine.TransactionService.exceptions.TransactionNotFoundException;
import com.LoyaltyEngine.TransactionService.exceptions.TransactionRepositoryException;
import com.LoyaltyEngine.TransactionService.models.enums.OutboxStatus;
import com.LoyaltyEngine.TransactionService.models.enums.Status;
import com.LoyaltyEngine.TransactionService.models.domain.TransactionDomain;
import com.LoyaltyEngine.TransactionService.models.domain.TransactionItemDomain;
import com.LoyaltyEngine.TransactionService.models.entity.OutboxEvent;
import com.LoyaltyEngine.TransactionService.models.entity.Transaction;
import com.LoyaltyEngine.TransactionService.models.eventModels.CancelTransactionEventModel;
import com.LoyaltyEngine.TransactionService.models.eventModels.TransactionCreatedEvent;
import com.LoyaltyEngine.TransactionService.models.eventModels.TransactionItemEvent;
import com.LoyaltyEngine.TransactionService.services.interfaces.OutboxEventRepository;
import com.LoyaltyEngine.TransactionService.services.interfaces.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.uuid.UuidCreator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.DataException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {
    private final ObjectMapper mapper;

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final OutboxEventRepository outboxEventRepository;

    @Value("${kafka.topics.transaction-created}")
    private String transactionCreatedTopic;

    @Value("${kafka.topics.transaction-cancel}")
    private String cancelTransactionTopic;

    private final MeterRegistry registry;
    private final Tracer tracer;

    @Transactional
    public TransactionDomain createTransaction(UUID userId, BigDecimal amount, List<TransactionItemDomain> items, UUID idempotencyKey, Boolean useCashback) {
        ScopedSpan span = tracer.startScopedSpan("transaction-create-span");
        Timer.Sample timer = Timer.start(registry);

        Optional<TransactionDomain> transactionByIdempotencyKey = getTransactionByIdempotencyKey(idempotencyKey);
        if (transactionByIdempotencyKey.isPresent()) {
            return transactionByIdempotencyKey.get();
        }

        TransactionDomain newTransaction = TransactionDomain.create(userId, idempotencyKey, amount, items, useCashback);
        UUID transactionId = newTransaction.getId().value();
        span.tag("transaction.id", transactionId.toString());

        List<TransactionItemEvent> eventItems = items
                .stream()
                .map(
                        item -> new TransactionItemEvent(
                                item.getCategory(),
                                item.getName(),
                                item.getPrice().amount()
                        )
                )
                .toList();

        TransactionCreatedEvent transactionCreated = new TransactionCreatedEvent(
                transactionId,
                userId,
                amount,
                eventItems,
                newTransaction.getCreatedAt(),
                useCashback
        );
        try {
            Transaction savedTransaction = transactionRepository.save(transactionMapper.transactionDomainToEntity(newTransaction));

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(UuidCreator.getTimeOrderedEpoch())
                    .aggregateId(transactionId)
                    .createdAt(LocalDateTime.now())
                    .eventType(transactionCreatedTopic)
                    .payload(mapper.writeValueAsString(transactionCreated))
                    .status(OutboxStatus.NEW)
                    .build();
            outboxEventRepository.save(outboxEvent);

            log.info("Successfully saved new trans. - id: {}", transactionId);

            span.tag("status", "SUCCESSFUL");
            registry.counter("transaction.create", "status", "successful").increment();

            return transactionMapper.transactionEntityToDomain(savedTransaction);
        } catch (DataException e) {
            span.error(e);
            span.tag("error.message", e.getMessage());
            span.tag("status", "FAILED");
            registry.counter("transaction.create", "status", "failed").increment();

            throw new TransactionRepositoryException("Error saving new trans.", e);
        } catch (JacksonException e) {
            span.error(e);
            span.tag("error.message", e.getMessage());
            span.tag("status", "FAILED");
            registry.counter("transaction.create", "status", "failed").increment();

            throw new TransactionMappingException("Error mapping", e);
        } catch (Exception e) {
            registry.counter("transaction.create", "status", "failed").increment();
            span.error(e);
            span.tag("error.message", e.getMessage());
            span.tag("status", "FAILED");

            log.error("Unexpected error creating transaction: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            span.end();
            timer.stop(registry.timer("transaction.create.duration"));
        }
    }

    public TransactionDomain getTransactionById(UUID id) {
        try {
            return transactionMapper
                    .transactionEntityToDomain(transactionRepository
                            .getTransactionById(id)
                            .orElseThrow(() -> new TransactionNotFoundException(String.format("Transaction %s not found", id)))
                    );
        } catch (TransactionNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting transaction by id: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public Optional<TransactionDomain> getTransactionByIdempotencyKey(UUID idempotencyKey) {
        try {
            return transactionRepository.getTransactionByIdempotencyKey(idempotencyKey).map(transactionMapper::transactionEntityToDomain);
        } catch (DataException e) {
            throw new TransactionRepositoryException("Error finding trans. by idempotencyKey", e);
        }
    }

    public List<TransactionDomain> getTransactionByUserId(UUID id) {
        try {
            List<Transaction> transactions = transactionRepository.getTransactionsByUserId(id);
            return transactions
                    .stream()
                    .map(transactionMapper::transactionEntityToDomain)
                    .toList();
        } catch (DataException e) {
            throw new TransactionRepositoryException("Error getting transaction", e);
        } catch (Exception e) {
            log.error("Unexpected error getting transaction by userId: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void updateStatus(Status status, UUID transactionId) {
        try {
            TransactionDomain transaction = transactionMapper.transactionEntityToDomain(transactionRepository
                    .getTransactionById(transactionId)
                    .orElseThrow(() -> new TransactionNotFoundException(String.format("Transaction %s not found", transactionId))));

            switch (status) {
                case PROCESSED -> transaction.completeTransaction();
                case REJECTED -> transaction.rejectTransaction();
                case CANCELLED -> transaction.cancelTransaction();
            }

            transactionRepository.save(transactionMapper.transactionDomainToEntity(transaction));
        } catch (TransactionNotFoundException | IllegalArgumentException e) {
            log.error(e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating status of transaction: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void cancelTransaction(UUID transactionId) {
        try {
            TransactionDomain transaction = getTransactionById(transactionId);
            if (transaction.getStatus().equals(Status.CANCELLED)) throw new IllegalArgumentException("Status already cancelled");
            if (!transaction.getStatus().equals(Status.PROCESSED)) throw new IllegalArgumentException("Cant change status not from processed");

            CancelTransactionEventModel eventModel = new CancelTransactionEventModel(
                    transactionId,
                    transaction.getUserId().value(),
                    transaction.getAmount().amount(),
                    transaction.getUseCashbackBalance()
            );

            OutboxEvent event = OutboxEvent.builder()
                    .id(UuidCreator.getTimeOrderedEpoch())
                    .eventType(cancelTransactionTopic)
                    .aggregateId(transactionId)
                    .payload(mapper.writeValueAsString(eventModel))
                    .createdAt(LocalDateTime.now())
                    .retryCount(0)
                    .status(OutboxStatus.NEW)
                    .build();
            outboxEventRepository.save(event);
        } catch (TransactionNotFoundException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error publishing event 'cancel transaction' for [{}]", transactionId);
            throw new RuntimeException(e);
        }
    }
}
