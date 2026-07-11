package com.ficmart.paymentgateway.payment.application.exceptions;

public class PaymentRefNotFoundException extends RuntimeException {

    public PaymentRefNotFoundException() {
        super("Payment reference not found");
    }
}