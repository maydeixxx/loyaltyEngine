package com.LoyaltyEngine.TransactionService.exceptions;

import org.hibernate.exception.DataException;

public class TransactionRepositoryException extends RuntimeException {
    public TransactionRepositoryException(String message, DataException e) {
        super(message);
    }
}
