package com.ficmart.paymentgateway.payment.infrastructure.bank.exception;


import org.springframework.http.HttpStatusCode;


public class BankAuthorizationException extends BankOperationException {


    public BankAuthorizationException(HttpStatusCode status, String errorCode, String message) {
        super(status, errorCode, message);
    }
}