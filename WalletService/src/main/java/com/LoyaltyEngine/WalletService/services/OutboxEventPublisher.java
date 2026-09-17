package com.LoyaltyEngine.WalletService.services;

import com.LoyaltyEngine.WalletService.models.entity.OutboxEvent;
import com.LoyaltyEngine.WalletService.models.enums.OutboxStatus;
import com.LoyaltyEngine.WalletService.services.interfaces.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OutboxEventPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<UUID, String> kafkaTemplate;

    public OutboxEventPublisher(OutboxEventRepository outboxEventRepository, @Qualifier("outboxEventPublisherKafkaTemplate") KafkaTemplate<UUID, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
    public void handleOutboxEvents() {
        try {
            List<OutboxEvent> outBoxEvents = outboxEventRepository.findOutBoxEvents(OutboxStatus.NEW);

            for (OutboxEvent event : outBoxEvents) {
                try {
                    UUID aggregateId = event.getAggregateId();
                    String topic = event.getEventType();
                    String payload = event.getPayload();

                    kafkaTemplate.send(topic, aggregateId, payload).get(5, TimeUnit.SECONDS);

                    event.setStatus(OutboxStatus.SENT);
                    event.setProcessedAt(LocalDateTime.now());
                    outboxEventRepository.save(event);
                } catch (Exception e) {
                    if (event.getRetryCount() >= 3) {
                        event.setStatus(OutboxStatus.FAILED);
                    }
                    event.setRetryCount(event.getRetryCount() + 1);
                    outboxEventRepository.save(event);
                    log.error("Error handling event: {} || aggregate id: {}, event type: {}", e.getMessage(), event.getAggregateId(), event.getEventType());
                }
            }
        }  catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
        }
    }
}
