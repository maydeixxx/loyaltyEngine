package com.LoyaltyEngine.TransactionService.services.interfaces;

import com.LoyaltyEngine.TransactionService.models.domain.TransactionDomain;
import com.LoyaltyEngine.TransactionService.models.domain.TransactionItemDomain;
import com.LoyaltyEngine.TransactionService.models.dto.CreateTransactionItem;
import com.LoyaltyEngine.TransactionService.models.dto.TransactionDTO;
import com.LoyaltyEngine.TransactionService.models.entity.Transaction;
import com.LoyaltyEngine.TransactionService.models.entity.TransactionItem;
import org.mapstruct.Mapper;

import java.util.Optional;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
    Transaction transactionDomainToEntity(TransactionDomain domain);
    TransactionDomain transactionEntityToDomain(Transaction entity);
    TransactionDTO transactionDomainToDTO(TransactionDomain transactionDomain);

    TransactionItem transactionItemDomainToEntity(TransactionItemDomain item);
    TransactionItemDomain createTransactionItemDtoToDomain(CreateTransactionItem item);
}
