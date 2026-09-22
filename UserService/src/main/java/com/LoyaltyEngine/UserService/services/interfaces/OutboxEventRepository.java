package com.LoyaltyEngine.UserService.services.interfaces;

import com.LoyaltyEngine.UserService.models.entity.OutboxEvent;
import com.LoyaltyEngine.UserService.models.enums.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query("""
    SELECT e FROM OutboxEvent e
    WHERE e.status = :status
    ORDER BY e.createdAt ASC
""")
    List<OutboxEvent> findOutboxEvents(OutboxStatus status, Pageable pageable);
}
