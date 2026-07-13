package com.ficmart.paymentgateway.payment.api.dto;

import jakarta.validation.constraints.NotBlank;

public record VoidRequest(
        @NotBlank(message = "Payment reference is required")
        String paymentReference
) {
}
