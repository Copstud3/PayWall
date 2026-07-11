package com.ficmart.paymentgateway.payment.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CaptureRequest(
        @NotBlank(message = "Payment reference is required")
        String paymentReference
) {
}
