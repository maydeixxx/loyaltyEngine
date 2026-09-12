package com.LoyaltyEngine.RuleEngineService.exceptions;

public class JwtCheckingException extends RuntimeException {
    public JwtCheckingException(String message) {
        super(message);
    }
}
