package com.LoyaltyEngine.WalletService.exceptions;

public class WalletExistsException extends RuntimeException {
    public WalletExistsException(String message) {
        super(message);
    }
}
