package com.LoyaltyEngine.TransactionService.services;

import com.LoyaltyEngine.TransactionService.models.enums.Status;
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
    private void handlePointsFailed(ConsumerRecord<UUID, PointsFailedEvent> record) {
        try {
            UUID transactionId = record.key();
            PointsFailedEvent pointsFailed = record.value();
            String cause = pointsFailed.getCause();
            LocalDateTime failedAt = pointsFailed.getFailedAt();
            UUID userId = pointsFailed.getUserId();
            BigDecimal amount = pointsFailed.getAmount();

            transactionService.updateStatus(Status.REJECTED, transactionId);
            log.info("``Новый статус транзакции [{}] - {} || Причина - {} || timestamp - {} || User id - {} || amount - {}", transactionId, Status.REJECTED, cause, failedAt, userId, amount);
        } catch (Exception e) {
            log.error("Error handling points failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.transaction-handled}",
            groupId = "transaction_service",
            containerFactory = "transactionHandledEventContainerFactory"
    )
    private void handleTransactionHandledEvent(ConsumerRecord<UUID, TransactionHandledEvent> record) {
        try {
            TransactionHandledEvent model = record.value();
            UUID transactionId = model.getTransactionId();
            UUID userId = model.getUserId();

            transactionService.updateStatus(Status.PROCESSED, transactionId);
            log.info("Transaction {} for user {} successfully handled!", transactionId, userId);
        } catch (Exception e) {
            log.error("Error handling transaction handled event: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}