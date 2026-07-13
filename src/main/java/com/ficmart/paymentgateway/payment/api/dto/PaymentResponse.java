package com.ficmart.paymentgateway.payment.api.dto;

import com.ficmart.paymentgateway.payment.domain.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResponse(
        String paymentReference,
        String orderId,
        String customerId,
        Long amountInCents,
        String currency,
        PaymentStatus status,
        String failureReason,
        LocalDateTime authorizedAt,
        LocalDateTime capturedAt,
        LocalDateTime voidedAt,
        LocalDateTime refundedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
