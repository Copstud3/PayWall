package com.ficmart.paymentgateway.payment.api;

import com.ficmart.paymentgateway.payment.api.dto.ErrorResponse;
import com.ficmart.paymentgateway.payment.api.dto.ValidationErrorResponse;
import com.ficmart.paymentgateway.payment.application.exceptions.*;
import com.ficmart.paymentgateway.payment.infrastructure.bank.dto.PaymentErrorResponse;
import com.ficmart.paymentgateway.payment.infrastructure.bank.exception.BankAuthorizationException;
import com.ficmart.paymentgateway.payment.infrastructure.bank.exception.BankCommunicationException;
import com.ficmart.paymentgateway.payment.infrastructure.bank.exception.BankOperationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
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

    @ExceptionHandler({
            PaymentAlreadyProcessedException.class,
            PaymentNotRefundableException.class,
            IdempotencyConflictException.class
    })
    public ResponseEntity<ErrorResponse> handlePaymentConflict(
            RuntimeException ex
    ) {

        ErrorResponse response = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                "PAYMENT_CONFLICT",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
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

    @ExceptionHandler(BankOperationException.class)
    public ResponseEntity<ErrorResponse> handleBankOperation(
            BankOperationException ex
    ) {

        HttpStatus status = ex.getStatus() != null
                ? (HttpStatus) ex.getStatus()
                : HttpStatus.BAD_GATEWAY;

        ErrorResponse response = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                ex.getErrorCode(),
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(status).body(response);
    }


    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeader(
            MissingRequestHeaderException ex
    ) {

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "MISSING_HEADER",
                ex.getHeaderName() + " header is required.",
                LocalDateTime.now()
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(BankCommunicationException.class)
    public ResponseEntity<ErrorResponse> handleBankCommunicationException(
            BankCommunicationException ex
    ) {

        ErrorResponse response = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                "BANK_UNAVAILABLE",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }

    @ExceptionHandler(PaymentOperationInProgressException.class)
    public ResponseEntity<ErrorResponse> handlePaymentOperationInProgress(
            PaymentOperationInProgressException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        HttpStatus.CONFLICT.value(),
                        "Conflict",
                        "OPERATION_IN_PROGRESS",
                        ex.getMessage(),
                        LocalDateTime.now()
                ));
    }
}