package com.LoyaltyEngine.RuleEngineService.api;

import com.LoyaltyEngine.RuleEngineService.exceptions.CashbackRuleNotFoundException;
import com.LoyaltyEngine.RuleEngineService.exceptions.CashbackRuleValidationException;
import com.LoyaltyEngine.RuleEngineService.models.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CashbackRuleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlerCashbackRuleNotFoundException(CashbackRuleNotFoundException e, WebRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .error("Rule not found")
                .code(404)
                .message(e.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(response.getCode()).body(response);
    }

    @ExceptionHandler(CashbackRuleValidationException.class)
    public ResponseEntity<ErrorResponse> handlerCashbackRuleValidationException(CashbackRuleValidationException e, WebRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .error("Not valid data")
                .message(e.getMessage())
                .code(400)
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(response.getCode()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handlerMethodArgumentNotValidException(MethodArgumentNotValidException e, WebRequest request) {
        String errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse response = ErrorResponse.builder()
                .error("Not valid data")
                .message(errors)
                .code(400)
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(response.getCode()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handlerException(Exception e, WebRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .error("Server error")
                .message(e.getMessage())
                .code(500)
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(response.getCode()).body(response);
    }
}
