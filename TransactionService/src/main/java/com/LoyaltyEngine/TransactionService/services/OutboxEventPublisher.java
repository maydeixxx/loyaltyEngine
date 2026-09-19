package com.LoyaltyEngine.TransactionService.services;

import com.LoyaltyEngine.TransactionService.models.entity.OutboxEvent;
import com.LoyaltyEngine.TransactionService.models.enums.OutboxStatus;
import com.LoyaltyEngine.TransactionService.models.eventModels.TransactionCreatedEvent;
import com.LoyaltyEngine.TransactionService.services.interfaces.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

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

    @Scheduled(fixedDelay = 5000)
    public void sendPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.NEW, PageRequest.of(0, 50));
        if (events.isEmpty()) {
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
                if (event.getRetryCount() >= 3) {
                    event.setStatus(OutboxStatus.FAILED);
                }
                event.setRetryCount(event.getRetryCount() + 1);
                outboxEventRepository.save(event);

                log.error("Error sending message: {}", e.getMessage());
            }
        }
    }
}
