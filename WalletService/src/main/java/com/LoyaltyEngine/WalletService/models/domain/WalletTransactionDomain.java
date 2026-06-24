package com.LoyaltyEngine.WalletService.models.domain;

import com.LoyaltyEngine.WalletService.exceptions.InvalidArgumentException;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@ToString
@EqualsAndHashCode
@Getter
@RequiredArgsConstructor
public class WalletTransactionDomain {
    @Setter
    private UUID id;
    private final UUID walletId;
    private final UUID transactionId;
    private final BigDecimal amount;
    private final TransactionType type;
    private final LocalDateTime createdAt;
    private final String description;

    public static WalletTransactionDomain createWalletTransaction(UUID walletId, UUID transactionId, BigDecimal amount, TransactionType type, LocalDateTime createdAt, String description) {
        if (walletId == null) {
            throw new InvalidArgumentException("Wallet id cant be null");
        }

        if (transactionId == null) {
            throw new InvalidArgumentException("Transaction id cant be null");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidArgumentException("Amount cant be null or less than zero");
        }

        if (type == null) {
            throw new InvalidArgumentException("Type cant be null");
        }

        if (createdAt == null || createdAt.isBefore(LocalDateTime.now())) {
            throw new InvalidArgumentException("Created time cant be null or before than present time");
        }

        if (description == null) {
            throw new InvalidArgumentException("Description cant be null");
        }

        return new WalletTransactionDomain(walletId, transactionId, amount, type, createdAt, description);
    }
}
