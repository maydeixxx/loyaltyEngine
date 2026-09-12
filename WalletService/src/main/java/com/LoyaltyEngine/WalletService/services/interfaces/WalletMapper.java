package com.LoyaltyEngine.WalletService.services.interfaces;

import com.LoyaltyEngine.WalletService.models.domain.WalletDomain;
import com.LoyaltyEngine.WalletService.models.entity.Wallet;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {
    public Wallet domainToEntity(WalletDomain domain) {
        Wallet wallet = new Wallet();
        wallet.setId(domain.getId().value());
        wallet.setUserId(domain.getUserId().value());
        wallet.setBalance(domain.getBalance().amount());
        wallet.setStatus(domain.getStatus());
        wallet.setCreatedAt(domain.getCreatedAt());
        wallet.setUpdatedAt(domain.getUpdatedAt());
        wallet.setVersion(domain.getVersion());

        return wallet;
    }

    public WalletDomain entityToDomain(Wallet entity) {
        return WalletDomain.restoreFromExisting(
                entity.getId(),
                entity.getUserId(),
                entity.getBalance(),
                entity.getStatus(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
