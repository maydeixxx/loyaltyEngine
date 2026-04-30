package com.LoyaltyEngine.TransactionService.entity.interfaces;

import com.LoyaltyEngine.TransactionService.domain.TransactionDomain;
import com.LoyaltyEngine.TransactionService.entity.Transaction;
import org.mapstruct.Mapper;

@Mapper
public interface TransactionMapper {
    Transaction transactionDomainToEntity(TransactionDomain domain);
    TransactionDomain transactionEntityToDomain(Transaction entity);
}
