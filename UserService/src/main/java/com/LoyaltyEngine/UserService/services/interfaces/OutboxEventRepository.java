package com.LoyaltyEngine.UserService.services.interfaces;

import com.LoyaltyEngine.UserService.models.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query(value = """
                SELECT * FROM outbox_events e
                WHERE e.status = 'NEW'
                AND e.retry_count <= 3
                ORDER BY e.created_at ASC
                LIMIT 50
                FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findNewOutBoxEvents();


    @Modifying
    @Query(value = """
                DELETE FROM outbox_events e
                WHERE (e.status = 'FAILED' OR e.status = 'SENT')
                AND e.processed_at < NOW() - interval '7 days'
            """, nativeQuery = true)
    void deleteOldEvents();
}
