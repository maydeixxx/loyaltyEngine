package com.LoyaltyEngine.TransactionService.api;

import com.LoyaltyEngine.TransactionService.models.domain.TransactionItemDomain;
import com.LoyaltyEngine.TransactionService.models.dto.CreateTransaction;
import com.LoyaltyEngine.TransactionService.models.dto.TransactionDTO;
import com.LoyaltyEngine.TransactionService.services.TransactionService;
import com.LoyaltyEngine.TransactionService.services.interfaces.TransactionMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    @PostMapping()
    public ResponseEntity<TransactionDTO> createTransaction(
            @RequestHeader(value = "X-IDEMPOTENCY-KEY", required = true) UUID idempotencyKey,
            @RequestBody @Valid CreateTransaction transaction
    ) {
        log.info(
                "Creating transaction for user: {}, amount: {}, items: {}",
                transaction.getUserId(), transaction.getAmount(), transaction.getItems()
        );

        List<TransactionItemDomain> domainItems = transaction.getItems().stream()
                .map(transactionMapper::createTransactionItemDtoToDomain)
                .toList();
        TransactionDTO savedTransaction = transactionMapper.transactionDomainToDTO(
                transactionService.createTransaction(
                        transaction.getUserId(), transaction.getAmount(), domainItems, idempotencyKey
                )
        );
        log.info("Transaction created successfully with id: {}", savedTransaction.getId());
        return ResponseEntity.status(201).body(savedTransaction);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByUserId(@PathVariable Long userId) {
        List<TransactionDTO> transactions = transactionService.getTransactionByUserId(userId).stream().map(transactionMapper::transactionDomainToDTO).toList();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> getTransactionById(@PathVariable UUID id) {
        TransactionDTO transactionById = transactionMapper.transactionDomainToDTO(transactionService.getTransactionById(id));
        return ResponseEntity.ok((transactionById));
    }

}
