package com.LoyaltyEngine.TransactionService.services.interfaces;

import com.LoyaltyEngine.TransactionService.models.domain.TransactionDomain;
import com.LoyaltyEngine.TransactionService.models.domain.TransactionItemDomain;
import com.LoyaltyEngine.TransactionService.models.dto.CreateTransactionItem;
import com.LoyaltyEngine.TransactionService.models.dto.TransactionDTO;
import com.LoyaltyEngine.TransactionService.models.entity.Transaction;
import com.LoyaltyEngine.TransactionService.models.entity.TransactionItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface TransactionMapper {
    @Mapping(target = "transactionItems", source = "items")
    Transaction transactionDomainToEntity(TransactionDomain domain);

    @Mapping(target = "items", source = "transactionItems")
    TransactionDomain transactionEntityToDomain(Transaction entity);

    TransactionDTO transactionDomainToDTO(TransactionDomain transactionDomain);

    @Mapping(target = "transaction", ignore = true)
    TransactionItem transactionItemDomainToEntity(TransactionItemDomain item);
    @Mapping(target = "id", ignore = true)
    TransactionItemDomain createTransactionItemDtoToDomain(CreateTransactionItem item);
}
