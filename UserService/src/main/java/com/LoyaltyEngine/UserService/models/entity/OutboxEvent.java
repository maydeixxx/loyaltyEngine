package com.LoyaltyEngine.UserService.models.entity;

import com.LoyaltyEngine.UserService.models.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutboxEvent {
    @Id
    @Column(nullable = false, unique = true)
    private UUID id;

    @Column(nullable = false)
    private UUID aggregateId;

    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    private String eventType;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;
    private int retryCount;


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

