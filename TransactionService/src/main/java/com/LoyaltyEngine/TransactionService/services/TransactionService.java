package com.LoyaltyEngine.TransactionService.services;

import com.LoyaltyEngine.TransactionService.exceptions.TransactionNotFoundException;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.uuid.UuidCreator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

            OutboxEvent outboxEvent = buildOutboxEvent(
                    transactionId,
                    transactionCreatedTopic,
                    transactionCreated
            );

            outboxEventRepository.save(outboxEvent);

            log.info("Successfully saved new trans. - id: {}", transactionId);

            span.tag("status", "SUCCESSFUL");
            registry.counter("transaction.create", "status", "successful").increment();

            return transactionMapper.transactionEntityToDomain(savedTransaction);
        } catch (Exception e) {
            registry.counter("transaction.create", "status", "failed").increment();
            span.error(e);
            span.tag("error.message", e.getMessage());
            span.tag("status", "FAILED");

            log.error("Unexpected error creating transaction: {}", e.getMessage());
            throw e;
        } finally {
            span.end();
            timer.stop(registry.timer("transaction.create.duration"));
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
        return transactionRepository.getTransactionByIdempotencyKey(idempotencyKey).map(transactionMapper::transactionEntityToDomain);
    }

    public List<TransactionDomain> getTransactionByUserId(UUID id) {
        List<Transaction> transactions = transactionRepository.getTransactionsByUserId(id);
        return transactions
                .stream()
                .map(transactionMapper::transactionEntityToDomain)
                .toList();
    }

    @Transactional
    public void updateStatus(Status status, UUID transactionId) {
        TransactionDomain transaction = transactionMapper.transactionEntityToDomain(transactionRepository
                .getTransactionById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(String.format("Transaction %s not found", transactionId))));

        switch (status) {
            case PROCESSED -> transaction.completeTransaction();
            case REJECTED -> transaction.rejectTransaction();
            case CANCELLED -> transaction.cancelTransaction();
            default -> throw new IllegalArgumentException("Unknown status");
        }

        transactionRepository.save(transactionMapper.transactionDomainToEntity(transaction));
    }

    @Transactional
    public void cancelTransaction(UUID transactionId) {
        TransactionDomain transaction = getTransactionById(transactionId);
        if (transaction.getStatus().equals(Status.CANCELLED))
            throw new IllegalArgumentException("Status already cancelled");
        if (!transaction.getStatus().equals(Status.PROCESSED))
            throw new IllegalArgumentException("Cant change status not from processed");

        CancelTransactionEventModel eventModel = new CancelTransactionEventModel(
                transactionId,
                transaction.getUserId().value(),
                transaction.getAmount().amount(),
                transaction.getUseCashbackBalance()
        );

        OutboxEvent event = buildOutboxEvent(
                transactionId,
                cancelTransactionTopic,
                eventModel
        );
        outboxEventRepository.save(event);
    }

    private OutboxEvent buildOutboxEvent(UUID aggregateId, String topic, Object payload) {
        try {
            return OutboxEvent.builder()
                    .id(UuidCreator.getTimeOrderedEpoch())
                    .aggregateId(aggregateId)
                    .createdAt(LocalDateTime.now())
                    .eventType(topic)
                    .payload(mapper.writeValueAsString(payload))
                    .status(OutboxStatus.NEW)
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cant serialize %s: ".formatted(payload), e);
        }

    }
}
