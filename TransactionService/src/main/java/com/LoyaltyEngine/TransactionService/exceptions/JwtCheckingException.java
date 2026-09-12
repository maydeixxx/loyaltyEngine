package com.LoyaltyEngine.TransactionService.exceptions;

public class JwtCheckingException extends RuntimeException {
    public JwtCheckingException(String message) {
        super(message);
    }
}
