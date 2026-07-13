package com.ficmart.paymentgateway.payment.infrastructure.bank.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class BankOperationException extends RuntimeException {

    private final HttpStatusCode status;
    private final String errorCode;

    public BankOperationException(
            HttpStatusCode status,
            String errorCode,
            String message
    ) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public BankOperationException(
            HttpStatusCode status,
            String errorCode,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }
}