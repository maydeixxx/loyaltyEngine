package com.LoyaltyEngine.WalletService.exceptions;

public class WalletTransactionNotFound extends RuntimeException {
    public WalletTransactionNotFound(String message) {
        super(message);
    }
}
