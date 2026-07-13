package com.ficmart.paymentgateway.payment.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RefundRequest(
        @NotBlank(message = "Payment Reference is required")
        String paymentReference
) {
}
