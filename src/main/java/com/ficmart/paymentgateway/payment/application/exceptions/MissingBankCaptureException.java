package com.ficmart.paymentgateway.payment.application.exceptions;

public class MissingBankCaptureException extends RuntimeException{
    public MissingBankCaptureException() {
        super("Payment cannot be refunded because no bank capture ID exists.");
    }
}
