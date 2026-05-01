package com.LoyaltyEngine.TransactionService.services.interfaces;

import com.LoyaltyEngine.TransactionService.models.domain.TransactionDomain;
import com.LoyaltyEngine.TransactionService.models.entity.Transaction;
import org.mapstruct.Mapper;

@Mapper
public interface TransactionMapper {
    Transaction transactionDomainToEntity(TransactionDomain domain);
    TransactionDomain transactionEntityToDomain(Transaction entity);
}
