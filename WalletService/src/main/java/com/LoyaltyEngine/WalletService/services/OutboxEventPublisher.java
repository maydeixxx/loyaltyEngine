package com.LoyaltyEngine.WalletService.services;

import com.LoyaltyEngine.WalletService.models.entity.OutboxEvent;
import com.LoyaltyEngine.WalletService.models.enums.OutboxStatus;
import com.LoyaltyEngine.WalletService.services.interfaces.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OutboxEventPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<UUID, String> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;

    public OutboxEventPublisher(OutboxEventRepository outboxEventRepository, @Qualifier("outboxEventPublisherKafkaTemplate") KafkaTemplate<UUID, String> kafkaTemplate, TransactionTemplate transactionTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void handleOutboxEvents() {
        try {
            List<OutboxEvent> outBoxEvents = claimEvents();
            if (outBoxEvents == null || outBoxEvents.isEmpty()) {
                return;
            }

            log.info("Found {} not handled events", outBoxEvents.size());

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
                    log.error("Error sending event [type = {}]: {}", event.getEventType(), e.getMessage());
                    if (event.getRetryCount() >= 3) {
                        event.setStatus(OutboxStatus.FAILED);
                    } else {
                        event.setStatus(OutboxStatus.NEW);
                    }
                    event.setRetryCount(event.getRetryCount() + 1);
                    outboxEventRepository.save(event);
                    log.error("Error handling event: {} || aggregate id: {}, event type: {}", e.getMessage(), event.getAggregateId(), event.getEventType());
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
        }
    }

    public List<OutboxEvent> claimEvents() {
        try {
            return transactionTemplate.execute(_ -> {
                List<OutboxEvent> outBoxEvents = outboxEventRepository.findNewOutBoxEvents();
                outBoxEvents.forEach(outboxEvent -> outboxEvent.setStatus(OutboxStatus.PROCESSING));
                outboxEventRepository.saveAllAndFlush(outBoxEvents);
                return outBoxEvents;
            });
        } catch (Exception e) {
            log.error("Error claiming new events: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    @Transactional
    public void deleteOldEvents() {
        try {
            outboxEventRepository.deleteOldEvents();
        } catch (Exception e) {
            log.error("Error deleting old events: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
