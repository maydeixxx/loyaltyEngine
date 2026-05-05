package com.LoyaltyEngine.TransactionService.exceptions;

public class TransactionRepositoryException extends RuntimeException {
    public TransactionRepositoryException(String message) {
        super(message);
    }
}
