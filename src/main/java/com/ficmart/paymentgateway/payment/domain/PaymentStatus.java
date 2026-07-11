package com.ficmart.paymentgateway.payment.domain;

public enum PaymentStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    VOIDED,
    REFUNDED,
    FAILED
}
