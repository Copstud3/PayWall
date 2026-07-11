package com.ficmart.paymentgateway.payment.application.exceptions;

public class PaymentAlreadyProcessedException extends RuntimeException {
    public PaymentAlreadyProcessedException() {
        super("Payment already processed");
    }
}
