package com.LoyaltyEngine.TransactionService.services;

import com.LoyaltyEngine.TransactionService.models.entity.OutboxEvent;
import com.LoyaltyEngine.TransactionService.models.eventModels.TransactionCreatedEvent;
import com.LoyaltyEngine.TransactionService.services.interfaces.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxEventPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<UUID, TransactionCreatedEvent> template;
    private final ObjectMapper mapper;

    @Scheduled(fixedDelay = 5000)
    public void sendPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findOutboxEventByProcessedFalseOrderByCreatedAtAsc();
        if (events.isEmpty()) {
            return;
        }

        log.info("Найдено {} необработанных событий", events.size());

        for (OutboxEvent event : events) {
            try {
                TransactionCreatedEvent payload = mapper.readValue(event.getPayload(), TransactionCreatedEvent.class);
                String topic = event.getEventType().replace(".", "_");
                UUID transactionId = event.getAggregateId();

                template.send(topic, transactionId, payload).whenComplete(
                        (result, ex) -> {
                            if (ex == null) {
                                event.setProcessed(true);
                                event.setProcessedAt(LocalDateTime.now());
                                outboxEventRepository.save(event);
                                log.info("Отправлено сообщение {} в топик {}", event.getId(), topic);
                            } else {
                                log.error("Ошибка при отправке сообщения {} в топик {}: {}", event.getId(), event.getEventType().replace(".", "_"), ex.getMessage());
                            }
                        }
                );
            } catch (JacksonException e) {
                log.error("Ошибка обработки payload: {}", e.getMessage());
            }
        }
    }
}
