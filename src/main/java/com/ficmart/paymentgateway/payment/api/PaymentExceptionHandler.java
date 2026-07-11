package com.ficmart.paymentgateway.payment.api;

import com.ficmart.paymentgateway.payment.api.dto.ValidationErrorResponse;
import com.ficmart.paymentgateway.payment.application.exceptions.IdempotencyConflictException;
import com.ficmart.paymentgateway.payment.application.exceptions.PaymentAlreadyProcessedException;
import com.ficmart.paymentgateway.payment.application.exceptions.PaymentRefNotFoundException;
import com.ficmart.paymentgateway.payment.infrastructure.bank.dto.PaymentErrorResponse;
import com.ficmart.paymentgateway.payment.infrastructure.bank.exception.BankAuthorizationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class PaymentExceptionHandler {

    @ExceptionHandler(PaymentRefNotFoundException.class)
    public ResponseEntity<String> handlePaymentNotFound(
            PaymentRefNotFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ex.getMessage());
    }

    @ExceptionHandler(PaymentAlreadyProcessedException.class)
    public ResponseEntity<String> handlePaymentAlreadyProcessed(
            PaymentAlreadyProcessedException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        fieldErrors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        ValidationErrorResponse response = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                "The request contains invalid fields",
                fieldErrors,
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(BankAuthorizationException.class)
    public ResponseEntity<PaymentErrorResponse> handleBankAuthorization(BankAuthorizationException ex) {
        var status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.SERVICE_UNAVAILABLE;
        var response = new PaymentErrorResponse(
                ex.getErrorCode(),
                ex.getMessage()
        );

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<String> handleIdempotencyConflict(IdempotencyConflictException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ex.getMessage());
    }
}