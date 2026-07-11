package com.ficmart.paymentgateway.payment.infrastructure.bank.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class BankAuthorizationException extends RuntimeException {

    private final HttpStatusCode status;
    private final String errorCode;

    public BankAuthorizationException(
            HttpStatusCode status,
            String errorCode,
            String message
    ) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}