package com.LoyaltyEngine.TransactionService.models.domain;

import com.LoyaltyEngine.TransactionService.models.domain.valueObjects.IdempotencyKey;
import com.LoyaltyEngine.TransactionService.models.domain.valueObjects.Money;
import com.LoyaltyEngine.TransactionService.models.domain.valueObjects.TransactionId;
import com.LoyaltyEngine.TransactionService.models.domain.valueObjects.UserId;
import com.LoyaltyEngine.TransactionService.models.enums.Status;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
public class TransactionDomain {
    private final TransactionId id;
    private final UserId userId;
    private final IdempotencyKey idempotencyKey;
    private final Money amount;
    private final List<TransactionItemDomain> items;
    private final LocalDateTime createdAt;
    private Status status;
    private final Boolean useCashbackBalance;

    private TransactionDomain(TransactionId id, UserId userId, IdempotencyKey idempotencyKey, Money amount, List<TransactionItemDomain> items, LocalDateTime createdAt, Status status, Boolean useCashbackBalance) {
        this.id = id;
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
        this.items = items;
        this.createdAt = createdAt;
        this.status = status;
        this.useCashbackBalance = useCashbackBalance;
    }

    public static TransactionDomain create(UUID rawUserId, UUID rawIdempotencyKey, BigDecimal amount, List<TransactionItemDomain> items, Boolean useCashbackBalance) {
        Money transactionAmount = Money.of(amount);
        Money totalSum = new Money(BigDecimal.ZERO);
        IdempotencyKey idempotencyKey = new IdempotencyKey(rawIdempotencyKey);
        UserId userId = new UserId(rawUserId);

        if (items.isEmpty()) {
            throw new IllegalArgumentException("Items size must be >= 1");
        }

        for (TransactionItemDomain item : items) {
            totalSum = totalSum.add(item.getPrice());
        }

        if (!useCashbackBalance && (transactionAmount.isLessThan(totalSum))) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        if (transactionAmount.isGreaterThan(totalSum)) {
            throw new IllegalArgumentException("Sum of the trans. cant be greater than amount of items");
        }

        TransactionId transactionId = TransactionId.generateTransactionId();
        return new TransactionDomain(transactionId, userId, idempotencyKey, transactionAmount, items, LocalDateTime.now(), Status.NEW, useCashbackBalance);
    }

    public static TransactionDomain restoreFromExisting(UUID transactionId, UUID userId, UUID idempotencyKey, BigDecimal amount, List<TransactionItemDomain> items, LocalDateTime createdAt, Status status, Boolean userCashbackBalance) {
        TransactionId transactionId1 = TransactionId.restoreFromExisting(transactionId);
        UserId userId1 = UserId.restoreFromExisting(userId);
        IdempotencyKey idempotencyKey1 = IdempotencyKey.restoreFromExisting(idempotencyKey);
        Money money = Money.of(amount);

        if (items.isEmpty()) {
            throw new IllegalArgumentException("Items size must be >= 1");
        }

        if (status == null) {
            throw new IllegalArgumentException("Status cant be null");
        }

        return new TransactionDomain(transactionId1, userId1, idempotencyKey1, money, items, createdAt, status, userCashbackBalance);
    }

    public void rejectTransaction() {
        if (!this.status.equals(Status.NEW)) throw new IllegalArgumentException("Can change status only from new");
        this.status = Status.REJECTED;
    }

    public void completeTransaction() {
        if (!this.status.equals(Status.NEW)) throw new IllegalArgumentException("Can change status only from new");
        this.status = Status.PROCESSED;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TransactionDomain that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
