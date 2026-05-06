package com.LoyaltyEngine.TransactionService.api;

import com.LoyaltyEngine.TransactionService.exceptions.TransactionCreatingException;
import com.LoyaltyEngine.TransactionService.exceptions.TransactionNotFoundException;
import com.LoyaltyEngine.TransactionService.exceptions.TransactionRepositoryException;
import com.LoyaltyEngine.TransactionService.models.apiResponses.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<?> transactionNotFoundHandler(TransactionNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = buildResponse("Transaction not found", new HashMap<>(), ex.getMessage(), 404, request);
        return ResponseEntity.status(404).body(errorResponse);
    }

    @ExceptionHandler(TransactionCreatingException.class)
    public ResponseEntity<?> transactionCreatingHandler(TransactionCreatingException ex, WebRequest request) {
        ErrorResponse errorResponse = buildResponse("Error while creating transaction", new HashMap<>(), ex.getMessage(), 400, request);
        return ResponseEntity.status(400).body(errorResponse);
    }

    @ExceptionHandler(TransactionRepositoryException.class)
    public ResponseEntity<?> transactionRepositoryHandler(TransactionRepositoryException ex, WebRequest request) {
        log.error("Error 500: ", ex);
        ErrorResponse errorResponse = buildResponse("Error in repository", new HashMap<>(), ex.getMessage(), 500, request);
        return ResponseEntity.status(500).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validationExceptionHandler(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
        ErrorResponse errorResponse = buildResponse("Validation Failed", errors, "Ошибка валидации данных", 400, request);

        return ResponseEntity.status(400).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> globalExceptionHandler(Exception ex, WebRequest request) {
        log.error("Error 500: ", ex);
        ErrorResponse errorResponse = buildResponse("Global Error", new HashMap<>(), "Непредвиденная ошибка на сервере", 500, request);
        return ResponseEntity.status(500).body(errorResponse);
    }

    private ErrorResponse buildResponse(String error, Map<String, String> errors, String message, int status, WebRequest request) {
        return ErrorResponse.builder()
                .error(error)
                .errors(errors)
                .message(message)
                .status(status)
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .build();
    }
}
