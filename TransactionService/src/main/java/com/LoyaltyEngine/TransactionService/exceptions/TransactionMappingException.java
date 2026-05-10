package com.LoyaltyEngine.TransactionService.exceptions;

import tools.jackson.databind.exc.JsonNodeException;

public class TransactionMappingException extends RuntimeException {
    public TransactionMappingException(String message, JsonNodeException e) {
        super(message);
    }
}
