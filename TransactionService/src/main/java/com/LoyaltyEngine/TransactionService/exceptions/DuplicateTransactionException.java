package com.LoyaltyEngine.TransactionService.exceptions;

public class DuplicateTransactionException extends RuntimeException{
    public DuplicateTransactionException(String message) {
        super(message);
    }
}
