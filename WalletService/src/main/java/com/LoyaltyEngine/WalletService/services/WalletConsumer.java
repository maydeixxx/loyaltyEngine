package com.LoyaltyEngine.WalletService.services;

import com.LoyaltyEngine.WalletService.exceptions.WalletBlockedException;
import com.LoyaltyEngine.WalletService.models.events.CalculatedCashbackEventModel;
import com.LoyaltyEngine.WalletService.models.events.PointsFailedEvent;
import com.LoyaltyEngine.WalletService.services.interfaces.WalletProducer;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletConsumer {
    private final WalletService walletService;
    private final WalletProducer walletProducer;

    @KafkaListener(
            topics = "points_calculated",
            groupId = "wallet_service",
            containerFactory = "calculatedCashbackEventModelConcurrentKafkaListenerContainerFactory"
    )
    private void handlePointsCalculatedEvent(ConsumerRecord<UUID, CalculatedCashbackEventModel> record, Acknowledgment ack) {
        UUID transactionId = record.key();
        CalculatedCashbackEventModel model = record.value();

        try {
            walletService.creditPoints(model.getUserId(), transactionId, model.getAmount());
            ack.acknowledge();
        } catch (WalletBlockedException e) {
            PointsFailedEvent pointsFailed = PointsFailedEvent.builder()
                    .userId(model.getUserId())
                    .cause("Wallet is blocked")
                    .amount(model.getAmount())
                    .failedAt(LocalDateTime.now())
                    .build();

            walletProducer.sendMessageToPointsFailed(transactionId, pointsFailed);
            ack.acknowledge();
        } catch (Exception e) {
            PointsFailedEvent pointsFailed = PointsFailedEvent.builder()
                    .userId(model.getUserId())
                    .cause("Wallet Error")
                    .amount(model.getAmount())
                    .failedAt(LocalDateTime.now())
                    .build();

            walletProducer.sendMessageToPointsFailed(transactionId, pointsFailed);
            ack.acknowledge();
        }
    }
}
