package com.LoyaltyEngine.TransactionService.api;

import com.LoyaltyEngine.TransactionService.exceptions.*;
import com.LoyaltyEngine.TransactionService.models.dto.ErrorResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<?> transactionNotFoundHandler(TransactionNotFoundException ex, WebRequest request) {
        ErrorResponseDTO errorResponseDTO = buildResponse("Transaction not found", ex.getMessage(), 404, request);
        return ResponseEntity.status(404).body(errorResponseDTO);
    }

    @ExceptionHandler(TransactionCreatingException.class)
    public ResponseEntity<?> transactionCreatingHandler(TransactionCreatingException ex, WebRequest request) {
        ErrorResponseDTO errorResponseDTO = buildResponse("Error while creating transaction", ex.getMessage(), 400, request);
        return ResponseEntity.status(400).body(errorResponseDTO);
    }

    @ExceptionHandler(TransactionRepositoryException.class)
    public ResponseEntity<?> transactionRepositoryHandler(TransactionRepositoryException ex, WebRequest request) {
        ErrorResponseDTO errorResponseDTO = buildResponse("Error in repository", ex.getMessage(), 500, request);
        return ResponseEntity.status(500).body(errorResponseDTO);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validationExceptionHandler(MethodArgumentNotValidException ex, WebRequest request) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        ErrorResponseDTO errorResponseDTO = buildResponse("Validation Failed", errors, 400, request);

        return ResponseEntity.status(400).body(errorResponseDTO);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> globalExceptionHandler(Exception ex, WebRequest request) {
        ErrorResponseDTO errorResponseDTO = buildResponse("Global Error", ex.getMessage(), 500, request);
        return ResponseEntity.status(500).body(errorResponseDTO);
    }

    @ExceptionHandler(TransactionMappingException.class)
    public ResponseEntity<?> handleTransactionMappingException(TransactionMappingException ex, WebRequest request) {
        ErrorResponseDTO errorResponseDTO = buildResponse("Error while mapping transaction", ex.getMessage(), 400, request);
        return ResponseEntity.status(400).body(errorResponseDTO);
    }

    private ErrorResponseDTO buildResponse(String error, String errorMessage, int status, WebRequest request) {
        return new ErrorResponseDTO(
                error,
                errorMessage,
                status,
                request.getDescription(false).replace("uri=", ""),
                LocalDateTime.now()
        );
    }
}
