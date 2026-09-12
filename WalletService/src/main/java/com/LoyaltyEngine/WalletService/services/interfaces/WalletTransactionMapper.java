package com.LoyaltyEngine.WalletService.services.interfaces;

import com.LoyaltyEngine.WalletService.models.domain.WalletTransactionDomain;
import com.LoyaltyEngine.WalletService.models.dto.WalletTransactionDto;
import com.LoyaltyEngine.WalletService.models.entity.WalletTransaction;
import org.springframework.stereotype.Component;

@Component
public class WalletTransactionMapper {
    public WalletTransaction domainToEntity(WalletTransactionDomain domain) {
        WalletTransaction walletTransaction = new WalletTransaction();
        walletTransaction.setId(domain.getId().value());
        walletTransaction.setWalletId(domain.getWalletId().value());
        walletTransaction.setTransactionId(domain.getTransactionId().value());
        walletTransaction.setAmount(domain.getAmount().amount());
        walletTransaction.setCreatedAt(domain.getCreatedAt());
        walletTransaction.setDescription(domain.getDescription());
        walletTransaction.setType(domain.getType());

        return walletTransaction;
    }

    public WalletTransactionDomain entityToDomain(WalletTransaction entity) {
       return WalletTransactionDomain.restoreFromExisting(
                entity.getId(),
                entity.getWalletId(),
                entity.getTransactionId(),
                entity.getAmount(),
                entity.getType(),
                entity.getCreatedAt(),
                entity.getDescription()
        );
    }

    public WalletTransactionDto domainToDto(WalletTransactionDomain walletDomain) {
        return new WalletTransactionDto(
                walletDomain.getId().value(),
                walletDomain.getWalletId().value(),
                walletDomain.getTransactionId().value(),
                walletDomain.getAmount().amount(),
                walletDomain.getType(),
                walletDomain.getCreatedAt(),
                walletDomain.getDescription()
        );
    }
}
