package com.LoyaltyEngine.UserService.exceptions;

public class JwtCheckingException extends RuntimeException {
    public JwtCheckingException(String message) {
        super(message);
    }
}
