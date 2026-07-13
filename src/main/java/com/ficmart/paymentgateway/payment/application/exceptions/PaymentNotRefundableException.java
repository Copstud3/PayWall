package com.ficmart.paymentgateway.payment.application.exceptions;

import com.ficmart.paymentgateway.payment.api.dto.RefundRequest;

public class PaymentNotRefundableException extends RuntimeException {
    public PaymentNotRefundableException() {
        super("Only captured payments can be refunded");
    }
}
