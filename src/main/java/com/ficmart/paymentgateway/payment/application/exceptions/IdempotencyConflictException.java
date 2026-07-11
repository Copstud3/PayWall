package com.ficmart.paymentgateway.payment.application.exceptions;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException() {
        super("The idempotency key has already been used for a different request");
    }
}