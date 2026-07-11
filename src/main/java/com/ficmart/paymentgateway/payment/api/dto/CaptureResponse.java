package com.ficmart.paymentgateway.payment.api.dto;

import com.ficmart.paymentgateway.payment.domain.PaymentStatus;

public record CaptureResponse(
        String paymentReference,
        PaymentStatus status,
        Long amountInCents,
        String currency,
        String message
) {
}
