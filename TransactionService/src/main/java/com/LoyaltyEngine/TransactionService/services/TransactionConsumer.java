package com.LoyaltyEngine.TransactionService.services;

import com.LoyaltyEngine.TransactionService.models.domain.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionConsumer {
    private final TransactionService transactionService;

    @KafkaListener(
            topics = "${kafka.topics.points-failed}",
            groupId = "transaction_service",
            containerFactory = "pointsFailedConsumerFactory"
    )
    private void handlePointsFailed(ConsumerRecord<UUID, String> record) {
        UUID transactionId = record.key();
        String cause = record.value();

        transactionService.updateStatus(Status.REJECTED, transactionId);
        log.info("Новый статус транзакции [{}] - {} || Причина - {}", transactionId, Status.REJECTED, cause);
    }
}