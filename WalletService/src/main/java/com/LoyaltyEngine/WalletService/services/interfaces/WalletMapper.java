package com.LoyaltyEngine.WalletService.services.interfaces;

import com.LoyaltyEngine.WalletService.models.domain.WalletDomain;
import com.LoyaltyEngine.WalletService.models.entity.Wallet;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WalletMapper {
    Wallet domainToEntity(WalletDomain domain);
}
