package com.ficmart.paymentgateway.payment.infrastructure.bank.exception;

public class BankCommunicationException extends RuntimeException {

    public BankCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public BankCommunicationException(String message) {
        super(message);
    }
}
