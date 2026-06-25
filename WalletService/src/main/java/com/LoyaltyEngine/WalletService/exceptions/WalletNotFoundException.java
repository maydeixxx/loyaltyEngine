package com.LoyaltyEngine.WalletService.exceptions;

public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException(String message) {
        super(message);
    }
}
