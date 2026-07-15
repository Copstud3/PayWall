package com.ficmart.paymentgateway.payment.application.exceptions;

public class PaymentOperationInProgressException extends RuntimeException {

    public PaymentOperationInProgressException() {
        super("A payment operation with this idempotency key is already in progress");
    }
}