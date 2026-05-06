package com.LoyaltyEngine.TransactionService.services.interfaces;

import com.LoyaltyEngine.TransactionService.models.domain.TransactionDomain;
import com.LoyaltyEngine.TransactionService.models.dto.TransactionDTO;
import com.LoyaltyEngine.TransactionService.models.entity.Transaction;
import org.mapstruct.Mapper;

import java.util.Optional;

@Mapper
public interface TransactionMapper {
    Transaction transactionDomainToEntity(TransactionDomain domain);
    TransactionDomain transactionEntityToDomain(Transaction entity);
    TransactionDTO transactionDomainToDTO(TransactionDomain transactionDomain);
    TransactionDTO transactionEntityToDTO(Transaction transactionDomain);
    Optional<TransactionDomain> transactionEntityToDomain(Optional<Transaction> transactionByIdempotencyKey);
}
