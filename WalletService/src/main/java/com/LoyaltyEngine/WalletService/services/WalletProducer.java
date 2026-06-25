package com.LoyaltyEngine.WalletService.services;

import com.LoyaltyEngine.WalletService.models.events.PointsFailedEvent;
import com.LoyaltyEngine.WalletService.models.events.TransactionHandledEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class WalletProducer {
    private final KafkaTemplate<UUID, PointsFailedEvent> kafkaTemplatePointsFailed;
    private final KafkaTemplate<UUID, TransactionHandledEvent> transactionHandledEventKafkaTemplate;

    public WalletProducer(
            @Qualifier("pointsFailedEventKafkaTemplate") KafkaTemplate<UUID, PointsFailedEvent> kafkaTemplatePointsFailed,
            @Qualifier("transactionHandledEventKafkaTemplate") KafkaTemplate<UUID, TransactionHandledEvent> transactionHandledEventKafkaTemplate
    ) {
        this.kafkaTemplatePointsFailed = kafkaTemplatePointsFailed;
        this.transactionHandledEventKafkaTemplate = transactionHandledEventKafkaTemplate;
    }

    public void sendMessageToPointsFailed(UUID transactionId, PointsFailedEvent event) {
        kafkaTemplatePointsFailed.send(
                        "points_failed",
                        transactionId,
                        event
                )
                .whenComplete(
                        (_, ex) -> {
                            if (ex == null) {
                                log.info("Message to topic points.failed sent successfully: {}", transactionId);
                            } else {
                                log.error("Error sending message to points.failed {}: {}", transactionId, ex.getMessage());
                            }
                        }
                );
    }

    public void sendHandledTransaction(UUID transactionId, Long userId) {
        TransactionHandledEvent event = TransactionHandledEvent.builder()
                .transactionId(transactionId)
                .userId(userId)
                .build();

        transactionHandledEventKafkaTemplate.send("transaction_handled", transactionId, event).whenComplete(
                (_, ex) -> {
                    if (ex != null) {
                        log.error("Error sending message to transaction_handled : {}", ex.getMessage());
                    } else {
                        log.info("Message successfully sent to transaction_handled");
                    }
                }
        );
    }
}
