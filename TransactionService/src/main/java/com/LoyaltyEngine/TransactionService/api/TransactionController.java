package com.LoyaltyEngine.TransactionService.api;

import com.LoyaltyEngine.TransactionService.exceptions.TransactionCreatingException;
import com.LoyaltyEngine.TransactionService.models.domain.TransactionDomain;
import com.LoyaltyEngine.TransactionService.models.dto.CreateTransaction;
import com.LoyaltyEngine.TransactionService.models.dto.TransactionDTO;
import com.LoyaltyEngine.TransactionService.models.entity.Transaction;
import com.LoyaltyEngine.TransactionService.services.TransactionService;
import com.LoyaltyEngine.TransactionService.services.interfaces.TransactionMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    @PostMapping()
    public ResponseEntity<?> createTransaction(
            @RequestHeader(value = "X-IDEMPOTENCY-KEY", required = true) UUID idempotencyKey,
            @RequestBody @Valid CreateTransaction transaction
    ) {
        Optional<Transaction> transactionIsPresent = transactionService.getTransactionByIdempotencyKey(idempotencyKey);
        if (transactionIsPresent.isPresent()) {
            return ResponseEntity.ok(transactionMapper.transactionEntityToDTO(transactionIsPresent.get()));
        }
        TransactionDTO savedTransaction = transactionMapper.transactionDomainToDTO(transactionService.createTransaction(transaction.getUserId(), transaction.getAmount(), idempotencyKey));
        return ResponseEntity.status(201).body(savedTransaction);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getTransactionsByUserId(@PathVariable Long userId) {
        List<TransactionDomain> transactions = transactionService.getTransactionByUserId(userId);
        return ResponseEntity.ok(transactions.stream().map(transactionMapper::transactionDomainToDTO).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTransactionById(@PathVariable UUID id) {
        TransactionDomain transactionById = transactionService.getTransactionById(id);
        return ResponseEntity.ok(transactionMapper.transactionDomainToDTO(transactionById));
    }

}
