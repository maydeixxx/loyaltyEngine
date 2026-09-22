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
        ErrorResponse response = buildErrorResponse("Rule not found", e.getMessage(), 404, request);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @ExceptionHandler(CashbackRuleValidationException.class)
    public ResponseEntity<ErrorResponse> handlerCashbackRuleValidationException(CashbackRuleValidationException e, WebRequest request) {
        ErrorResponse response = buildErrorResponse("Not valid data", e.getMessage(), 400, request);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handlerMethodArgumentNotValidException(MethodArgumentNotValidException e, WebRequest request) {
        String errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse response = buildErrorResponse("Not valid data", errors, 400, request);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        ErrorResponse errorResponse = buildErrorResponse("Illegal argument", ex.getMessage(), 400, request);
        return ResponseEntity.status(errorResponse.getCode()).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handlerException(Exception e, WebRequest request) {
        ErrorResponse response = buildErrorResponse("Server error", e.getMessage(), 500, request);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    private ErrorResponse buildErrorResponse(String error, String message, int code, WebRequest request) {
        return ErrorResponse.builder()
                .error(error)
                .message(message)
                .code(code)
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .build();
    }
}
