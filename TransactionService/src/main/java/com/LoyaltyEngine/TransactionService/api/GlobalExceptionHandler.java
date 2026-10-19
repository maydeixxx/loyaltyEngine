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
    public ResponseEntity<ErrorResponseDTO> transactionNotFoundHandler(TransactionNotFoundException ex, WebRequest request) {
        log.error("Transaction not found: ", ex);
        ErrorResponseDTO errorResponseDTO = buildResponse("Transaction not found", ex.getMessage(), 404, request);
        return ResponseEntity.status(404).body(errorResponseDTO);
    }

    @ExceptionHandler(TransactionCreatingException.class)
    public ResponseEntity<?> transactionCreatingHandler(TransactionCreatingException ex, WebRequest request) {
        log.error("Error creating transaction: ", ex);
        ErrorResponseDTO errorResponseDTO = buildResponse("Error while creating transaction", ex.getMessage(), 400, request);
        return ResponseEntity.status(400).body(errorResponseDTO);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> validationExceptionHandler(MethodArgumentNotValidException ex, WebRequest request) {
        log.error("Validation error: ", ex);
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        ErrorResponseDTO errorResponseDTO = buildResponse("Validation Failed", errors, 400, request);

        return ResponseEntity.status(400).body(errorResponseDTO);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> globalExceptionHandler(Exception ex, WebRequest request) {
        log.error("Global error: ", ex);
        ErrorResponseDTO errorResponseDTO = buildResponse("Global Error", ex.getMessage(), 500, request);
        return ResponseEntity.status(500).body(errorResponseDTO);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.error("Illegal argument: ", ex);
        ErrorResponseDTO errorResponseDTO = buildResponse("Illegal argument", ex.getMessage(), 400, request);
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
