package com.LoyaltyEngine.UserService.services.interfaces;

import com.LoyaltyEngine.UserService.models.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query(value = """
                SELECT * FROM outbox_events e
                WHERE e.status = 'NEW'
                ORDER BY e.created_at ASC
                LIMIT 50
                FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findNewOutBoxEvents();
}
