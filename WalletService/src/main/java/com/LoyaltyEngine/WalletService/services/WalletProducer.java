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
    private final KafkaTemplate<UUID, Long> userRequestKafkaTemplate;

    public WalletProducer(
            @Qualifier("pointsFailedEventKafkaTemplate") KafkaTemplate<UUID, PointsFailedEvent> kafkaTemplatePointsFailed,
            @Qualifier("transactionHandledEventKafkaTemplate") KafkaTemplate<UUID, TransactionHandledEvent> transactionHandledEventKafkaTemplate,
            @Qualifier("userRequestEventModelKafkaTemplate") KafkaTemplate<UUID, Long> userRequestKafkaTemplate
    ) {
        this.kafkaTemplatePointsFailed = kafkaTemplatePointsFailed;
        this.transactionHandledEventKafkaTemplate = transactionHandledEventKafkaTemplate;
        this.userRequestKafkaTemplate = userRequestKafkaTemplate;
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

    public void sendUserRequest(UUID requestKey, Long userId) {
        userRequestKafkaTemplate.send("get_user_status", requestKey, userId).whenComplete(
                (_, ex) -> {
                    if (ex != null) {
                        log.error("Error sending userRequest: {}", ex.getMessage());
                    } else {
                        log.info("Message successfully sent to get_user_status");
                    }
                }
        );
    }
}
