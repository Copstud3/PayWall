package com.ficmart.paymentgateway.payment.api.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
        int status,
        String error,
        String code,
        String message,
        LocalDateTime timestamp
) {
}