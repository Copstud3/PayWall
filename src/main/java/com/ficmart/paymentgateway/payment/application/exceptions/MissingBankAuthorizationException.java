package com.ficmart.paymentgateway.payment.application.exceptions;

public class MissingBankAuthorizationException extends RuntimeException {
    public MissingBankAuthorizationException() {
        super("Payment cannot be captured because no bank authorization ID exists.");
    }
}
