package com.LoyaltyEngine.UserService.api;

import com.LoyaltyEngine.UserService.exceptions.*;
import com.LoyaltyEngine.UserService.models.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> authExceptionHandler(AuthenticationException ex, WebRequest request) {
        ErrorResponse errorResponse =  ErrorResponse.builder()
                .status(401)
                .error("authentication error")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(401).body(errorResponse);
    }

    @ExceptionHandler(CreateUserException.class)
    public ResponseEntity<ErrorResponse> createUserExceptionHandler(CreateUserException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(400)
                .error("error creating user")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(400).body(errorResponse);
    }

    @ExceptionHandler(DeleteUserException.class)
    public ResponseEntity<ErrorResponse> deleteUserExceptionHandler(DeleteUserException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(400)
                .error("error deleting user")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(400).body(errorResponse);
    }

    @ExceptionHandler(JwtCheckingException.class)
    public ResponseEntity<ErrorResponse> jwtCheckingExceptionHandler(JwtCheckingException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(409)
                .error("error checking jwt")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(409).body(errorResponse);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> userNotFoundExceptionHandler(UserNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(404)
                .error("failed to find user")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(404).body(errorResponse);
    }

    @ExceptionHandler(UserValidationException.class)
    public ResponseEntity<ErrorResponse> userValidationExceptionHandler(UserValidationException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(400)
                .error("error validating user")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(400).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> methodArgumentExceptionHandler(MethodArgumentNotValidException ex, WebRequest request) {
        String errors = ex.getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(400)
                .error("argument nor valid")
                .message(errors)
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(400).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(Exception ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(500)
                .error("internal server error")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(500).body(errorResponse);
    }

}
