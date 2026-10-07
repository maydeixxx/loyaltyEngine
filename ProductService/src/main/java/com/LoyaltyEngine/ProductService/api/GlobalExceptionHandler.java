package com.LoyaltyEngine.ProductService.api;

import com.LoyaltyEngine.ProductService.exceptions.ProductDeletingException;
import com.LoyaltyEngine.ProductService.exceptions.ProductNotFoundException;
import com.LoyaltyEngine.ProductService.models.dtos.ErrorResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductDeletingException.class)
    public ResponseEntity<ErrorResponseDTO> handleProductDeletingException(ProductDeletingException ex, WebRequest request) {
        ErrorResponseDTO response = buildErrorResponse("Error deleting product", ex.getMessage(), 400, request);
        return ResponseEntity.status(response.code()).body(response);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleProductNotFoundException(ProductNotFoundException ex, WebRequest request) {
        ErrorResponseDTO response = buildErrorResponse("Product not found", ex.getMessage(), 404, request);
        return ResponseEntity.status(response.code()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, WebRequest request) {
        String errorMessage = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponseDTO response = buildErrorResponse("Validation error", errorMessage, 400, request);
        return ResponseEntity.status(response.code()).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        ErrorResponseDTO errorResponse = buildErrorResponse("Illegal argument", ex.getMessage(), 400, request);
        return ResponseEntity.status(errorResponse.code()).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handlerException(Exception e, WebRequest request) {
        ErrorResponseDTO response = buildErrorResponse("Server error", e.getMessage(), 500, request);
        return ResponseEntity.status(response.code()).body(response);
    }

    private ErrorResponseDTO buildErrorResponse(String error, String message, int code, WebRequest request) {
        return new ErrorResponseDTO(
                error,
                message,
                code,
                request.getDescription(false).replace("uri=", ""),
                LocalDateTime.now()
        );
    }
}
