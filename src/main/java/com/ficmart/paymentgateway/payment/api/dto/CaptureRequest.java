package com.ficmart.paymentgateway.payment.api.dto;

public record CaptureRequest(
        String paymentReference
) {
}
