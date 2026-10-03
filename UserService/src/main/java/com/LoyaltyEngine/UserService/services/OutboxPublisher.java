package com.LoyaltyEngine.UserService.services;

import com.LoyaltyEngine.UserService.models.entity.OutboxEvent;
import com.LoyaltyEngine.UserService.models.enums.OutboxStatus;
import com.LoyaltyEngine.UserService.services.interfaces.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OutboxPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<UUID, String> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, @Qualifier("outboxEventKafkaTemplate") KafkaTemplate<UUID, String> kafkaTemplate, TransactionTemplate transactionTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(fixedDelay = 5L, timeUnit = TimeUnit.SECONDS)
    public void handleOutboxEvents() {
        try {
            List<OutboxEvent> outboxEvents = claimEvents();
            if (outboxEvents == null || outboxEvents.isEmpty()) {
                return;
            }

            log.info("Found {} not handled events", outboxEvents.size());

            for (OutboxEvent event : outboxEvents) {
                try {
                    kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload()).get(10, TimeUnit.SECONDS);
                    event.setStatus(OutboxStatus.SENT);
                    log.info("Sent new message in {}", event.getEventType());
                    outboxEventRepository.save(event);
                } catch (Exception e) {
                    int retryCount = event.getRetryCount();
                    log.error("Error sending event [type = {}]: {}", event.getEventType(), e.getMessage());
                    if (retryCount >= 3) {
                        event.setStatus(OutboxStatus.FAILED);
                    } else {
                        event.setStatus(OutboxStatus.NEW);
                    }
                    event.setRetryCount(retryCount + 1);
                    outboxEventRepository.save(event);
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
        }
    }

    public List<OutboxEvent> claimEvents() {
        try {
            return transactionTemplate.execute(_ -> {
                List<OutboxEvent> events = outboxEventRepository.findNewOutBoxEvents();
                events.forEach(event -> event.setStatus(OutboxStatus.PROCESSING));
                outboxEventRepository.saveAllAndFlush(events);
                return events;
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
