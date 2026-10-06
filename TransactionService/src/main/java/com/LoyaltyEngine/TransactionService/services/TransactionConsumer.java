package com.LoyaltyEngine.TransactionService.services;

import com.LoyaltyEngine.TransactionService.models.enums.Status;
import com.LoyaltyEngine.TransactionService.models.eventModels.CancellationFailedEvent;
import com.LoyaltyEngine.TransactionService.models.eventModels.PointsFailedEvent;
import com.LoyaltyEngine.TransactionService.models.eventModels.TransactionHandledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionConsumer {
    private final TransactionService transactionService;

    @KafkaListener(
            topics = "${kafka.topics.points-failed}",
            groupId = "transaction_service",
            containerFactory = "pointsFailedEventConcurrentKafkaListenerContainerFactory"
    )
    public void handlePointsFailed(ConsumerRecord<UUID, PointsFailedEvent> record) {
        UUID transactionId = record.key();
        PointsFailedEvent pointsFailed = record.value();
        String cause = pointsFailed.cause();
        LocalDateTime failedAt = pointsFailed.failedAt();
        UUID userId = pointsFailed.userId();
        BigDecimal amount = pointsFailed.amount();

        transactionService.updateStatus(Status.REJECTED, transactionId);
        log.info("New status of transaction [{}] - {} || Cause - {} || Timestamp - {} || User id - {} || Amount - {}", transactionId, Status.REJECTED, cause, failedAt, userId, amount);
    }

    @KafkaListener(
            topics = "${kafka.topics.transaction-handled}",
            groupId = "transaction_service",
            containerFactory = "transactionHandledEventContainerFactory"
    )
    public void handleTransactionHandledEvent(ConsumerRecord<UUID, TransactionHandledEvent> record) {
        TransactionHandledEvent model = record.value();
        UUID transactionId = model.transactionId();
        UUID userId = model.userId();

        transactionService.updateStatus(Status.PROCESSED, transactionId);
        log.info("Transaction {} for user {} successfully handled!", transactionId, userId);
    }

    @KafkaListener(
            topics = "${kafka.topics.transaction-cancellation-handled}",
            groupId = "transaction_service",
            containerFactory = "transactionHandledEventContainerFactory"
    )
    public void handleTransactionCancellation(ConsumerRecord<UUID, TransactionHandledEvent> record) {
        TransactionHandledEvent model = record.value();
        UUID transactionId = model.transactionId();
        UUID userId = model.userId();

        transactionService.updateStatus(Status.CANCELLED, transactionId);
        log.info("Transaction {} for user {} successfully cancelled!", transactionId, userId);
    }

    @KafkaListener(
            topics = "${kafka.topics.transaction-cancellation-failed}",
            groupId = "transaction_service",
            containerFactory = "cancellationFailedEventConcurrentKafkaListenerContainerFactory"
    )
    public void handleCancellationFailed(ConsumerRecord<UUID, CancellationFailedEvent> record) {
        UUID transactionId = record.key();
        CancellationFailedEvent cancellationFailedEvent = record.value();
        String cause = cancellationFailedEvent.cause();
        LocalDateTime failedAt = cancellationFailedEvent.failedAt();
        UUID userId = cancellationFailedEvent.userId();

        log.info("Error cancellation transaction [{}] || Cause - {} || Timestamp - {} || User id - {}", transactionId, cause, failedAt, userId);
    }
}