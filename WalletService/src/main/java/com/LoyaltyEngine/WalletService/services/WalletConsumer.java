package com.LoyaltyEngine.WalletService.services;

import com.LoyaltyEngine.WalletService.exceptions.WalletExistsException;
import com.LoyaltyEngine.WalletService.models.events.CalculatedCashbackEventModel;
import com.LoyaltyEngine.WalletService.models.events.CancelTransactionEventModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletConsumer {
    private final WalletService walletService;

    @KafkaListener(
            topics = "${kafka.topics.points-calculated}",
            groupId = "wallet_service",
            containerFactory = "calculatedCashbackEventModelConcurrentKafkaListenerContainerFactory"
    )
    public void handlePointsCalculatedEvent(ConsumerRecord<UUID, CalculatedCashbackEventModel> record, Acknowledgment ack) {
        UUID transactionId = record.key();
        CalculatedCashbackEventModel model = record.value();
        UUID userId = model.userId();

        walletService.creditPoints(userId, transactionId, model.amount(), model.useCashback(), model.amountOfTransaction(), model.totalItemPrice());
        ack.acknowledge();
    }

    @KafkaListener(
            topics = "${kafka.topics.user-created}",
            groupId = "wallet_service",
            containerFactory = "userCreatedKafkaListenerContainerFactory"
    )
    public void handleUserCreatedEvent(ConsumerRecord<UUID, String> record) {
        if (record.value() == null || record.value().isBlank()) {
            throw new IllegalArgumentException("User id is null");
        }

        UUID userId = UUID.fromString(record.value());

        try {
            walletService.createWallet(userId);
        } catch (WalletExistsException e) {
            log.warn("Wallet for user [{}] already created", userId);
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.transaction-cancel}",
            groupId = "wallet_service",
            containerFactory = "cancelTransactionEventModelConcurrentKafkaListenerContainerFactory"
    )
    public void handleTransactionCancellation(ConsumerRecord<UUID, CancelTransactionEventModel> record) {
        UUID transactionId = record.key();
        walletService.cancelTransaction(transactionId, record.value());
    }
}
