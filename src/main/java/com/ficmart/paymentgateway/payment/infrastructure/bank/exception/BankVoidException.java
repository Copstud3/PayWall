package com.ficmart.paymentgateway.payment.infrastructure.bank.exception;

import org.springframework.http.HttpStatusCode;

public class BankVoidException extends BankOperationException{
    public BankVoidException(HttpStatusCode status, String errorCode, String message) {
        super(status, errorCode, message);
    }
}
