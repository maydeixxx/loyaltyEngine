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

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CashbackRuleNotFoundException.class)
    public ResponseEntity<?> handlerCashbackRuleNotFoundException(CashbackRuleNotFoundException e, WebRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .error("Не найдено правило")
                .code(404)
                .message(e.getMessage())
                .path(request.getContextPath().replace("uri", ""))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(response.getCode()).body(response);
    }

    @ExceptionHandler(CashbackRuleValidationException.class)
    public ResponseEntity<?> handlerCashbackRuleValidationException(CashbackRuleValidationException e, WebRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .error("Невалидные данные")
                .message(e.getMessage())
                .code(400)
                .path(request.getContextPath().replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(response.getCode()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handlerMethodArgumentNotValidException(MethodArgumentNotValidException e, WebRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .error("Невалидные данные")
                .message(e.getMessage())
                .code(400)
                .path(request.getContextPath().replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(response.getCode()).body(response);
    }
}
