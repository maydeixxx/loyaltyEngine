package com.LoyaltyEngine.WalletService.services.interfaces;

import com.LoyaltyEngine.WalletService.models.events.PointsFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletProducer {
    @Qualifier("pointsFailedEventKafkaTemplate")
    private final KafkaTemplate<UUID, PointsFailedEvent> kafkaTemplate;

    public void sendMessageToPointsFailed(UUID transactionId, PointsFailedEvent event) {
        kafkaTemplate.send(
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
}
