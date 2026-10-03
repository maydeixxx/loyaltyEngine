package com.LoyaltyEngine.TransactionService.services;

import com.LoyaltyEngine.TransactionService.models.entity.OutboxEvent;
import com.LoyaltyEngine.TransactionService.models.enums.OutboxStatus;
import com.LoyaltyEngine.TransactionService.models.eventModels.TransactionCreatedEvent;
import com.LoyaltyEngine.TransactionService.services.interfaces.OutboxEventRepository;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxEventPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<UUID, TransactionCreatedEvent> template;
    private final ObjectMapper mapper;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(fixedDelay = 5000)
    public void sendPendingEvents() {
        try {

            List<OutboxEvent> events = claimNewEvents();
            if (events == null || events.isEmpty()) {
                return;
            }

            log.info("Found {} not handled events", events.size());

            for (OutboxEvent event : events) {
                try {
                    TransactionCreatedEvent payload = mapper.readValue(event.getPayload(), TransactionCreatedEvent.class);
                    String topic = event.getEventType().replace(".", "_");
                    UUID transactionId = event.getAggregateId();

                    template.send(topic, transactionId, payload).get(5, TimeUnit.SECONDS);
                    event.setStatus(OutboxStatus.SENT);
                    outboxEventRepository.save(event);

                    log.info("Message sent in {}. Transaction - {}", topic, transactionId);
                } catch (JacksonException e) {
                    event.setStatus(OutboxStatus.FAILED);
                    outboxEventRepository.save(event);
                    log.error("Error handling payload: {}", e.getMessage());
                } catch (Exception e) {
                    log.error("Error sending event [type = {}]: {}", event.getEventType(), e.getMessage());
                    if (event.getRetryCount() >= 3) {
                        event.setStatus(OutboxStatus.FAILED);
                    } else {
                        event.setStatus(OutboxStatus.NEW);
                    }
                    event.setRetryCount(event.getRetryCount() + 1);
                    outboxEventRepository.save(event);
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
        }
    }

    public List<OutboxEvent> claimNewEvents() {
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
