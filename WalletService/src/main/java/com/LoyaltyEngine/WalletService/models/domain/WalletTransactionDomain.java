package com.LoyaltyEngine.WalletService.models.domain;

import com.LoyaltyEngine.WalletService.exceptions.InvalidArgumentException;
import com.LoyaltyEngine.WalletService.models.enums.TransactionType;
import com.LoyaltyEngine.WalletService.models.domain.valueObjects.Money;
import com.LoyaltyEngine.WalletService.models.domain.valueObjects.TransactionId;
import com.LoyaltyEngine.WalletService.models.domain.valueObjects.WalletId;
import com.LoyaltyEngine.WalletService.models.domain.valueObjects.WalletTransactionId;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
public class WalletTransactionDomain {
    private final WalletTransactionId id;
    private final WalletId walletId;
    private final TransactionId transactionId;
    private final Money amount;
    private final TransactionType type;
    private final LocalDateTime createdAt;
    private final String description;

    private WalletTransactionDomain(WalletTransactionId id, WalletId walletId, TransactionId transactionId, Money amount, TransactionType type, LocalDateTime createdAt, String description) {
        if (type == null) {
            throw new InvalidArgumentException("Type cant be null");
        }

        if (createdAt == null) {
            throw new InvalidArgumentException("Created time cant be null or before than present time");
        }

        if (description == null) {
            throw new InvalidArgumentException("Description cant be null");
        }

        this.id = id;
        this.walletId = walletId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.type = type;
        this.createdAt = createdAt;
        this.description = description;
    }

    public static WalletTransactionDomain createWalletTransaction(UUID rawWalletId, UUID rawTransactionId, BigDecimal amount, TransactionType type, LocalDateTime createdAt, String description) {
        WalletTransactionId walletTransactionId = WalletTransactionId.generateId();
        WalletId walletId = new WalletId(rawWalletId);
        TransactionId transactionId = new TransactionId(rawTransactionId);
        Money money = new Money(amount);

        return new WalletTransactionDomain(walletTransactionId, walletId, transactionId, money, type, createdAt, description);
    }

    public static WalletTransactionDomain restoreFromExisting(UUID rawId, UUID rawWalletId, UUID rawTransactionId, BigDecimal amount, TransactionType type, LocalDateTime createdAt, String description) {
        WalletTransactionId walletTransactionId = new WalletTransactionId(rawId);
        WalletId walletId = new WalletId(rawWalletId);
        TransactionId transactionId = new TransactionId(rawTransactionId);
        Money money = new Money(amount);

        return new WalletTransactionDomain(walletTransactionId, walletId, transactionId, money, type, createdAt, description);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WalletTransactionDomain that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
