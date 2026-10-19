package com.LoyaltyEngine.UserService.api;

import com.LoyaltyEngine.UserService.exceptions.*;
import com.LoyaltyEngine.UserService.models.dto.ErrorResponse;
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

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> authExceptionHandler(AuthenticationException ex, WebRequest request) {
        log.error("Authentication error: ", ex);
        ErrorResponse errorResponse = buildErrorResponse(401, "authentication error", ex.getMessage(), request);
        return ResponseEntity.status(401).body(errorResponse);
    }

    @ExceptionHandler(CreateUserException.class)
    public ResponseEntity<ErrorResponse> createUserExceptionHandler(CreateUserException ex, WebRequest request) {
        log.error("Error creating user: ", ex);

        ErrorResponse errorResponse = buildErrorResponse(400, "error creating user", ex.getMessage(), request);
        return ResponseEntity.status(400).body(errorResponse);
    }

    @ExceptionHandler(DeleteUserException.class)
    public ResponseEntity<ErrorResponse> deleteUserExceptionHandler(DeleteUserException ex, WebRequest request) {
        log.error("Error deleting user: ", ex);

        ErrorResponse errorResponse = buildErrorResponse(400, "error deleting user", ex.getMessage(), request);
        return ResponseEntity.status(400).body(errorResponse);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> userNotFoundExceptionHandler(UserNotFoundException ex, WebRequest request) {
        log.error("User not found: ", ex);
        ErrorResponse errorResponse = buildErrorResponse(404, "failed to find user", ex.getMessage(), request);
        return ResponseEntity.status(404).body(errorResponse);
    }

    @ExceptionHandler(UserValidationException.class)
    public ResponseEntity<ErrorResponse> userValidationExceptionHandler(UserValidationException ex, WebRequest request) {
        log.error("User validation error: ", ex);
        ErrorResponse errorResponse = buildErrorResponse(400, "error validating user", ex.getMessage(), request);
        return ResponseEntity.status(400).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> methodArgumentExceptionHandler(MethodArgumentNotValidException ex, WebRequest request) {
        log.error("Validation error: ", ex);
        String errors = ex.getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse errorResponse = buildErrorResponse(400, "argument is not valid", errors, request);
        return ResponseEntity.status(400).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(Exception ex, WebRequest request) {
        log.error("Server error: ", ex);
        ErrorResponse errorResponse = buildErrorResponse(500, "internal server error", ex.getMessage(), request);
        return ResponseEntity.status(500).body(errorResponse);
    }

    @ExceptionHandler(UserUpdateException.class)
    public ResponseEntity<ErrorResponse> userUpdateExceptionHandler(UserUpdateException ex, WebRequest request) {
        log.error("Error updating user: ", ex);
        ErrorResponse errorResponse = buildErrorResponse(400, "error updating user", ex.getMessage(), request);
        return ResponseEntity.status(400).body(errorResponse);
    }

    private ErrorResponse buildErrorResponse(int status, String error, String errorMessage, WebRequest request) {
        return new ErrorResponse(
                status,
                error,
                errorMessage,
                request.getDescription(false).replace("uri=", ""),
                LocalDateTime.now()
        );
    }

}
