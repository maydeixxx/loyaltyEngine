package com.LoyaltyEngine.TransactionService.services.interfaces;

import com.LoyaltyEngine.TransactionService.models.domain.TransactionDomain;
import com.LoyaltyEngine.TransactionService.models.domain.TransactionItemDomain;
import com.LoyaltyEngine.TransactionService.models.dto.CreateTransactionItem;
import com.LoyaltyEngine.TransactionService.models.dto.TransactionDTO;
import com.LoyaltyEngine.TransactionService.models.dto.TransactionItemDTO;
import com.LoyaltyEngine.TransactionService.models.entity.Transaction;
import com.LoyaltyEngine.TransactionService.models.entity.TransactionItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;


@Component
public interface TransactionMapper {

    default Transaction transactionDomainToEntity(TransactionDomain domain) {
        Transaction transaction = new Transaction();
        transaction.setAmount(domain.getAmount().amount());
        transaction.setCurrency(domain.getAmount().currency().toString());
        transaction.setCreatedAt(domain.getCreatedAt());
        transaction.setId(domain.getId().value());
        transaction.setIdempotencyKey(domain.getIdempotencyKey().value());
        transaction.setStatus(domain.getStatus());

        transaction.setUserId(domain.getUserId().value());
        for (TransactionItemDomain item : domain.getItems()) {
            TransactionItem transactionItem = transactionItemDomainToEntity(item, transaction);
            transaction.addItem(transactionItem);
        }

        return transaction;
    }

    default TransactionDomain transactionEntityToDomain(Transaction entity) {
        List<TransactionItemDomain> items = new ArrayList<>();
        for (TransactionItem item : entity.getTransactionItems()) {
            items.add(transactionItemEntityToDomain(item));
        }

        return TransactionDomain.restoreFromExisting(
                entity.getId(),
                entity.getUserId(),
                entity.getIdempotencyKey(),
                entity.getAmount(),
                Currency.getInstance(entity.getCurrency()),
                items,
                entity.getCreatedAt(),
                entity.getStatus(),
                entity.getUseCashbackBalance()
        );
    }

    default TransactionDTO transactionDomainToDTO(TransactionDomain transactionDomain) {
        List<TransactionItemDTO> items = new ArrayList<>();
        for (TransactionItemDomain item : transactionDomain.getItems()) {
            items.add(transactionItemDomainToDTO(item));
        }

        return new TransactionDTO(
                transactionDomain.getId().value(),
                transactionDomain.getUserId().value(),
                transactionDomain.getIdempotencyKey().value(),
                transactionDomain.getAmount().amount(),
                items,
                transactionDomain.getCreatedAt(),
                transactionDomain.getStatus()
        );
    }

    default TransactionItemDTO transactionItemDomainToDTO(TransactionItemDomain item) {
        return new TransactionItemDTO(
                item.getCategory(),
                item.getName(),
                item.getPrice().amount()
        );
    }

    default TransactionItem transactionItemDomainToEntity(TransactionItemDomain item, Transaction parent) {
        TransactionItem transactionItem = new TransactionItem();
        transactionItem.setPrice(item.getPrice().amount());
        transactionItem.setCurrency(item.getPrice().currency().toString());
        transactionItem.setName(item.getName());
        transactionItem.setCategory(item.getCategory());
        transactionItem.setId(item.getId().value());
        transactionItem.setTransaction(parent);

        return transactionItem;
    }

    default TransactionItemDomain transactionItemEntityToDomain(TransactionItem transactionItem) {
        return TransactionItemDomain.restoreFromExisting(
                transactionItem.getId(),
                transactionItem.getCategory(),
                transactionItem.getName(),
                transactionItem.getPrice(),
                Currency.getInstance(transactionItem.getCurrency())
        );
    }

    default TransactionItemDomain transactionItemDtoToDomain(CreateTransactionItem item) {
        return TransactionItemDomain.createTransactionItem(
                item.category(),
                item.name(),
                item.price(),
                item.currency()
        );
    }
}
