package com.ficmart.paymentgateway.payment.infrastructure.bank.exception;

import org.springframework.http.HttpStatusCode;

public class BankRefundException extends BankOperationException{
    public BankRefundException(HttpStatusCode status, String errorCode, String message) {
        super(status, errorCode, message);
    }

}
