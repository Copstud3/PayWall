package com.ficmart.paymentgateway.payment.api.dto;

import com.ficmart.paymentgateway.payment.domain.PaymentStatus;

import java.time.Instant;
import java.time.LocalDateTime;

public record VoidResponse(
    String paymentReference,
    PaymentStatus status,
    LocalDateTime voidedAt
) {
}
