package com.LoyaltyEngine.TransactionService.models.entity;

import com.LoyaltyEngine.TransactionService.models.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class OutboxEvent {
    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID aggregateId;

    private String eventType;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;
    private int retryCount;
    private LocalDateTime processedAt;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OutboxEvent that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
