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
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("authentication.principal.userId == #userId()")
    public ResponseEntity<TransactionDTO> createTransaction(
            @RequestHeader(value = "X-IDEMPOTENCY-KEY") UUID idempotencyKey,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody @Valid CreateTransaction transaction
    ) {
        log.info(
                "Creating transaction for user: {}, amount: {}, items: {}",
                userId, transaction.amount(), transaction.items()
        );

        List<TransactionItemDomain> domainItems = transaction.items().stream()
                .map(transactionMapper::transactionItemDtoToDomain)
                .toList();
        TransactionDTO savedTransaction = transactionMapper.transactionDomainToDTO(
                transactionService.createTransaction(
                        userId, transaction.amount(), domainItems, idempotencyKey, transaction.useCashbackBalance()
                )
        );
        log.info("Transaction created successfully with id: {}", savedTransaction.id());
        return ResponseEntity.status(201).body(savedTransaction);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("authentication.principal.userId == #userId or hasRole('ADMIN')")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByUserId(@PathVariable UUID userId) {
        List<TransactionDTO> transactions = transactionService.getTransactionByUserId(userId).stream().map(transactionMapper::transactionDomainToDTO).toList();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransactionDTO> getTransactionById(@PathVariable UUID id) {
        TransactionDTO transactionById = transactionMapper.transactionDomainToDTO(transactionService.getTransactionById(id));
        return ResponseEntity.ok((transactionById));
    }

}
