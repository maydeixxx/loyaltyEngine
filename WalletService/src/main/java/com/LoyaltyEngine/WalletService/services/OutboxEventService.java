package com.LoyaltyEngine.WalletService.services;

import com.LoyaltyEngine.WalletService.models.entity.OutboxEvent;
import com.LoyaltyEngine.WalletService.services.interfaces.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventService {
    private final OutboxEventRepository eventRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveNewOutboxEvent(OutboxEvent event) {
        try {
            eventRepository.save(event);
        } catch (Exception e) {
            log.error("Error saving new event: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }


}
