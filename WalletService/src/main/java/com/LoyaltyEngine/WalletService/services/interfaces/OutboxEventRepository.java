package com.LoyaltyEngine.WalletService.services.interfaces;

import com.LoyaltyEngine.WalletService.models.entity.OutboxEvent;
import com.LoyaltyEngine.WalletService.models.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query("""
            SELECT e FROM OutboxEvent e
            WHERE e.status = :status
            AND e.retryCount <= 3
            ORDER BY e.createdAt ASC
            LIMIT 50
            """)
    List<OutboxEvent> findOutBoxEvents(OutboxStatus status);
}
