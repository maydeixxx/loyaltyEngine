package com.LoyaltyEngine.UserService.services;

import com.LoyaltyEngine.UserService.models.entity.OutboxEvent;
import com.LoyaltyEngine.UserService.models.enums.OutboxStatus;
import com.LoyaltyEngine.UserService.services.interfaces.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OutboxPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<UUID, String> kafkaTemplate;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, @Qualifier("outboxEventKafkaTemplate") KafkaTemplate<UUID, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 5L, timeUnit = TimeUnit.SECONDS)
    public void handleOutboxEvents() {
        try {
            List<OutboxEvent> outboxEvents = outboxEventRepository.findOutboxEvents(OutboxStatus.NEW, PageRequest.of(0, 50));

            if (outboxEvents.isEmpty()) return;

            for (OutboxEvent event : outboxEvents) {
                try {
                    kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload()).get(10, TimeUnit.SECONDS);
                    event.setStatus(OutboxStatus.SENT);
                    log.info("Sent nes message in {}", event.getEventType());
                    outboxEventRepository.save(event);
                } catch (Exception e) {
                    int retryCount = event.getRetryCount();
                    log.error("Error sending event [type = {}]", event.getEventType());
                    if (retryCount >= 3) {
                        event.setStatus(OutboxStatus.FAILED);
                    }
                    event.setRetryCount(retryCount + 1);
                    outboxEventRepository.save(event);
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
        }
    }
}
