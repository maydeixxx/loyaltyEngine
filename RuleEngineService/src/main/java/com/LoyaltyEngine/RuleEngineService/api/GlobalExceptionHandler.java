package com.LoyaltyEngine.RuleEngineService.api;

import com.LoyaltyEngine.RuleEngineService.exceptions.CashbackRuleNotFoundException;
import com.LoyaltyEngine.RuleEngineService.exceptions.CashbackRuleValidationException;
import com.LoyaltyEngine.RuleEngineService.exceptions.CashbackUpdateException;
import com.LoyaltyEngine.RuleEngineService.models.dto.ErrorResponse;
import jakarta.persistence.EntityExistsException;
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

    @ExceptionHandler(CashbackRuleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlerCashbackRuleNotFoundException(CashbackRuleNotFoundException ex, WebRequest request) {
        log.error("Rule not found: ", ex);
        ErrorResponse response = buildErrorResponse("Rule not found", ex.getMessage(), 404, request);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @ExceptionHandler(EntityExistsException.class)
    public ResponseEntity<ErrorResponse> handleEntityExistsException(EntityExistsException ex, WebRequest request) {
        log.error("Rule exists: ", ex);
        ErrorResponse response = buildErrorResponse("Rule exists", ex.getMessage(), 409, request);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @ExceptionHandler(CashbackRuleValidationException.class)
    public ResponseEntity<ErrorResponse> handlerCashbackRuleValidationException(CashbackRuleValidationException ex, WebRequest request) {
        log.error("Cashback model validation error: ", ex);
        ErrorResponse response = buildErrorResponse("Not valid data", ex.getMessage(), 400, request);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @ExceptionHandler(CashbackUpdateException.class)
    public ResponseEntity<ErrorResponse> handlerCashbackUpdateException(Exception ex, WebRequest request) {
        log.error("Update error: ", ex);
        ErrorResponse response = buildErrorResponse("Error updating cashback rule", ex.getMessage(), 400, request);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handlerMethodArgumentNotValidException(MethodArgumentNotValidException ex, WebRequest request) {
        log.error("Validation error: ", ex);
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse response = buildErrorResponse("Not valid data", errors, 400, request);
        return ResponseEntity.status(response.code()).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.error("Illegal argument: ", ex);
        ErrorResponse errorResponse = buildErrorResponse("Illegal argument", ex.getMessage(), 400, request);
        return ResponseEntity.status(errorResponse.code()).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handlerException(Exception ex, WebRequest request) {
        log.error("Server error: ", ex);
        ErrorResponse response = buildErrorResponse("Server error", ex.getMessage(), 500, request);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    private ErrorResponse buildErrorResponse(String error, String message, int code, WebRequest request) {
        return new ErrorResponse(
                error,
                message,
                code,
                request.getDescription(false).replace("uri=", ""),
                LocalDateTime.now()
        );
    }
}
