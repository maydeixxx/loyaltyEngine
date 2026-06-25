package com.LoyaltyEngine.WalletService.api;

import com.LoyaltyEngine.WalletService.models.dto.WalletTransactionDto;
import com.LoyaltyEngine.WalletService.services.WalletService;
import com.LoyaltyEngine.WalletService.services.interfaces.WalletTransactionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {
    private final WalletService walletService;
    private final WalletTransactionMapper walletTransactionMapper;

    @GetMapping("/{userId}/balance")
    public ResponseEntity<BigDecimal> getWalletBalance(@PathVariable Long userId) {
        return ResponseEntity.ok().body(walletService.getBalance(userId));
    }

    @GetMapping("/{userId}/history")
    public ResponseEntity<List<WalletTransactionDto>> getWalletHistory(@PathVariable Long userId) {
        List<WalletTransactionDto> walletTransactions = walletService.getTransactionsHistory(userId)
                .stream()
                .map(walletTransactionMapper::domainToDto)
                .toList();

        return ResponseEntity.ok().body(walletTransactions);
    }
}
