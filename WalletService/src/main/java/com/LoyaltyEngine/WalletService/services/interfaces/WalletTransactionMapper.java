package com.LoyaltyEngine.WalletService.services.interfaces;

import com.LoyaltyEngine.WalletService.models.domain.WalletTransactionDomain;
import com.LoyaltyEngine.WalletService.models.dto.WalletTransactionDto;
import com.LoyaltyEngine.WalletService.models.entity.WalletTransaction;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WalletTransactionMapper {
    WalletTransaction domainToEntity(WalletTransactionDomain domain);
    WalletTransactionDomain entityToDomain(WalletTransaction entity);
    WalletTransactionDto domainToDto(WalletTransactionDomain walletDomain);
}
