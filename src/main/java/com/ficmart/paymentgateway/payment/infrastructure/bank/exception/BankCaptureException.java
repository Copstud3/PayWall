package com.ficmart.paymentgateway.payment.infrastructure.bank.exception;

import org.springframework.http.HttpStatusCode;

public class BankCaptureException extends BankOperationException{
    public BankCaptureException(HttpStatusCode status, String errorCode, String message) {
        super(status, errorCode, message);
    }
}
