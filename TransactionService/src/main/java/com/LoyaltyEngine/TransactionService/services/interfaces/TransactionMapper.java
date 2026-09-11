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

        return TransactionDTO.builder()
                .amount(transactionDomain.getAmount().amount())
                .createdAt(transactionDomain.getCreatedAt())
                .id(transactionDomain.getId().value())
                .idempotencyKey(transactionDomain.getIdempotencyKey().value())
                .items(items)
                .status(transactionDomain.getStatus())
                .userId(transactionDomain.getUserId().value())
                .build();
    }

    default TransactionItemDTO transactionItemDomainToDTO(TransactionItemDomain item) {
        return TransactionItemDTO.builder()
                .name(item.getName())
                .price(item.getPrice().amount())
                .category(item.getCategory())
                .build();
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
                item.getCategory(),
                item.getName(),
                item.getPrice(),
                item.getCurrency()
        );
    }
}
