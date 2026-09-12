package com.LoyaltyEngine.WalletService.services;

import com.LoyaltyEngine.WalletService.exceptions.InsufficientFundsException;
import com.LoyaltyEngine.WalletService.exceptions.WalletBlockedException;
import com.LoyaltyEngine.WalletService.models.events.CalculatedCashbackEventModel;
import com.LoyaltyEngine.WalletService.models.events.PointsFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@SuppressWarnings("ALL")
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletConsumer {
    private final WalletService walletService;
    private final WalletProducer walletProducer;

    @KafkaListener(
            topics = "${kafka.topics.points-calculated}",
            groupId = "wallet_service",
            containerFactory = "calculatedCashbackEventModelConcurrentKafkaListenerContainerFactory"
    )
    private void handlePointsCalculatedEvent(ConsumerRecord<UUID, CalculatedCashbackEventModel> record, Acknowledgment ack) {
        UUID transactionId = record.key();
        CalculatedCashbackEventModel model = record.value();
        UUID userId = model.userId();

        try {
            walletService.creditPoints(userId, transactionId, model.amount(), model.useCashback(), model.amountOfTransaction(), model.totalItemPrice());
            ack.acknowledge();
            walletProducer.sendHandledTransaction(transactionId, userId);
        } catch (WalletBlockedException e) {
            PointsFailedEvent pointsFailedEvent = new PointsFailedEvent(
                    transactionId,
                    userId,
                    model.amount(),
                    "Wallet is blocked",
                    LocalDateTime.now()
            );

            walletProducer.sendMessageToPointsFailed(transactionId, pointsFailedEvent);
            ack.acknowledge();
        } catch (InsufficientFundsException e) {
            PointsFailedEvent pointsFailedEvent = new PointsFailedEvent(
                    transactionId,
                    userId,
                    model.amount(),
                    "Insufficient funds",
                    LocalDateTime.now()
            );

            walletProducer.sendMessageToPointsFailed(transactionId, pointsFailedEvent);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing cashback for transaction {} : {}", transactionId, e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
