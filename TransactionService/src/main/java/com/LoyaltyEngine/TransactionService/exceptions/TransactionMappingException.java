package com.LoyaltyEngine.TransactionService.exceptions;

import tools.jackson.core.JacksonException;

public class TransactionMappingException extends RuntimeException {
    public TransactionMappingException(String message, JacksonException e) {
        super(message);
    }
}
