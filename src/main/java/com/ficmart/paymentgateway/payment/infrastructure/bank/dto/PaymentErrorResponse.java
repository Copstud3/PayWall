package com.ficmart.paymentgateway.payment.infrastructure.bank.dto;

public record PaymentErrorResponse (
        String errorCode,
        String message
) {
}
