package com.LoyaltyEngine.WalletService.models.domain;

import com.LoyaltyEngine.WalletService.exceptions.InsufficientFundsException;
import com.LoyaltyEngine.WalletService.exceptions.WalletBlockedException;
import com.LoyaltyEngine.WalletService.models.enums.WalletStatus;
import com.LoyaltyEngine.WalletService.models.domain.valueObjects.Money;
import com.LoyaltyEngine.WalletService.models.domain.valueObjects.UserId;
import com.LoyaltyEngine.WalletService.models.domain.valueObjects.WalletId;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
public class WalletDomain {
    private final WalletId id;
    private final UserId userId;
    private Money balance;
    private WalletStatus status;
    private final Long version;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WalletDomain(WalletId walletId, UserId userId, Money balance, WalletStatus status, Long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.balance = balance;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.id = walletId;
    }

    public static WalletDomain createWallet(UUID rawUserId) {
        WalletId walletId = WalletId.generateId();
        UserId userId = new UserId(rawUserId);
        Money balance = Money.zeroOf();
        LocalDateTime now = LocalDateTime.now();

        return new WalletDomain(walletId, userId, balance, WalletStatus.ACTIVE, null, now, now);
    }

    public static WalletDomain restoreFromExisting(UUID rawWalletId, UUID rawUserId, BigDecimal amount, WalletStatus status, Long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        WalletId walletId = new WalletId(rawWalletId);
        UserId userId = new UserId(rawUserId);
        Money balance = new Money(amount);

        return new WalletDomain(walletId, userId, balance, status, version, createdAt, updatedAt);
    }

    public void activateWallet() {
        if (this.status.equals(WalletStatus.ACTIVE)) throw new IllegalArgumentException("Wallet already active");
        this.status = WalletStatus.ACTIVE;
        updateUpdatedAt(LocalDateTime.now());
    }

    public void blockWallet() {
        if (this.status.equals(WalletStatus.BLOCKED)) throw new IllegalArgumentException("Wallet already blocked");
        this.status = WalletStatus.BLOCKED;
        updateUpdatedAt(LocalDateTime.now());
    }

    public void suspendWallet() {
        if (this.status.equals(WalletStatus.SUSPENDED)) throw new IllegalArgumentException("Wallet already suspended");
        this.status = WalletStatus.SUSPENDED;
        updateUpdatedAt(LocalDateTime.now());
    }

    public void updateUpdatedAt(LocalDateTime time) {
        if (time == null) {
            throw new IllegalArgumentException("New time cant be null");
        }
        this.updatedAt = time;
    }

    public void credit(BigDecimal amount) {
        if (!this.status.equals(WalletStatus.ACTIVE)) throw new WalletBlockedException("Wallet isn`t active");
        this.balance = this.balance.add(new Money(amount));
        updateUpdatedAt(LocalDateTime.now());
    }

    public void debit(BigDecimal amount) {
        if (!this.status.equals(WalletStatus.ACTIVE)) throw new WalletBlockedException("Wallet isn`t active");
        if (this.balance.isLessThan(new Money(amount))) throw new InsufficientFundsException("Insufficient funds");
        this.balance = this.balance.subtract(new Money(amount));
        updateUpdatedAt(LocalDateTime.now());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WalletDomain that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
