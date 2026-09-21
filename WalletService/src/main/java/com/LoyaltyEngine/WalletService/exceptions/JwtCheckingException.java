package com.LoyaltyEngine.WalletService.exceptions;

public class JwtCheckingException extends RuntimeException {
    public JwtCheckingException(String message) {
        super(message);
    }
}
