package com.LoyaltyEngine.TransactionService.exceptions;

public class TransactionKafkaException extends RuntimeException {
    public TransactionKafkaException(String message) {
        super(message);
    }
}
