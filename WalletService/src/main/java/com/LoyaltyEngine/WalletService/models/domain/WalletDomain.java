package com.LoyaltyEngine.WalletService.models.domain;

import com.LoyaltyEngine.WalletService.exceptions.InvalidArgumentException;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class WalletDomain {
    @Setter
    private UUID id;
    private final Long userId;
    private final BigDecimal balance;
    private final WalletStatus status;
    private final Long version;
    private final LocalDateTime updatedAt;

    public static WalletDomain createWallet(Long userId) {
        if (userId == null || userId <= 0) {
            throw new InvalidArgumentException("User id is null or less than zero");
        }

        return new WalletDomain(
                userId,
                BigDecimal.ZERO,
                WalletStatus.ACTIVE,
                0L,
                LocalDateTime.now()
        );
    }
}
