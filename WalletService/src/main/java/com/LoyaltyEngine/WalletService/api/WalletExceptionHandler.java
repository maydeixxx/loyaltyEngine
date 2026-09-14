package com.LoyaltyEngine.WalletService.api;

import com.LoyaltyEngine.WalletService.exceptions.*;
import com.LoyaltyEngine.WalletService.models.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class WalletExceptionHandler {

    @ExceptionHandler(InvalidArgumentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidArgumentException(InvalidArgumentException ex, WebRequest request) {
        ErrorResponse errorResponse = buildErrorResponse(400, "Invalid argument", ex.getMessage(), request);
        return ResponseEntity.status(errorResponse.code()).body(errorResponse);
    }

    @ExceptionHandler(WalletBlockedException.class)
    public ResponseEntity<ErrorResponse> handleWalletBlockedException(WalletBlockedException ex, WebRequest request) {
        ErrorResponse errorResponse = buildErrorResponse(409, "Wallet is blocked", ex.getMessage(), request);
        return ResponseEntity.status(errorResponse.code()).body(errorResponse);
    }

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWalletNotFoundException(WalletNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = buildErrorResponse(404, "Wallet not found", ex.getMessage(), request);
        return ResponseEntity.status(errorResponse.code()).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validationExceptionError(MethodArgumentNotValidException ex, WebRequest request) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse errorResponse = buildErrorResponse(400, "Error validating data", errors, request);
        return ResponseEntity.status(errorResponse.code()).body(errorResponse);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFundsException(InsufficientFundsException ex, WebRequest request) {
        ErrorResponse errorResponse = buildErrorResponse(400, "Insufficent funds", ex.getMessage(), request);
        return ResponseEntity.status(errorResponse.code()).body(errorResponse);
    }

    @ExceptionHandler(WalletExistsException.class)
    public ResponseEntity<ErrorResponse> handleWalletExistsException(WalletExistsException ex, WebRequest request) {
        ErrorResponse errorResponse = buildErrorResponse(409, "wallet exists", ex.getMessage(), request);
        return ResponseEntity.status(errorResponse.code()).body(errorResponse);
    }

    private ErrorResponse buildErrorResponse(int status, String error, String errorMessage, WebRequest request) {
        return ErrorResponse.builder()
                .code(status)
                .error(error)
                .message(errorMessage)
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .build();
    }
}
