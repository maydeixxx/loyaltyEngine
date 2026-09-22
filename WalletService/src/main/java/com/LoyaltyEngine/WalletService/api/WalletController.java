package com.LoyaltyEngine.WalletService.api;

import com.LoyaltyEngine.WalletService.models.dto.CreateWalletDTO;
import com.LoyaltyEngine.WalletService.models.dto.WalletTransactionDto;
import com.LoyaltyEngine.WalletService.services.WalletService;
import com.LoyaltyEngine.WalletService.services.interfaces.WalletTransactionMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {
    private final WalletService walletService;
    private final WalletTransactionMapper walletTransactionMapper;

    @PostMapping()
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.userId == #walletDTO.userId()")
    public ResponseEntity<Void> createWallet(@RequestBody @Valid CreateWalletDTO walletDTO) {
        UUID userId = walletDTO.userId();

        walletService.createWallet(userId);
        return ResponseEntity.status(201).build();
    }

    @GetMapping("/{userId}/balance")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.userId == #userId")
    public ResponseEntity<BigDecimal> getWalletBalance(@PathVariable UUID userId) {
        return ResponseEntity.ok().body(walletService.getBalance(userId));
    }

    @GetMapping("/{userId}/history")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.userId == #userId")
    public ResponseEntity<List<WalletTransactionDto>> getWalletHistory(@PathVariable UUID userId) {
        List<WalletTransactionDto> walletTransactions = walletService.getTransactionsHistory(userId)
                .stream()
                .map(walletTransactionMapper::domainToDto)
                .toList();

        return ResponseEntity.ok().body(walletTransactions);
    }

    @PutMapping("/{userId}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> blockWallet(@PathVariable UUID userId) {
        walletService.blockWallet(userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{userId}/unblock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unblockWallet(@PathVariable UUID userId) {
        walletService.unblockWallet(userId);
        return ResponseEntity.ok().build();
    }
}
