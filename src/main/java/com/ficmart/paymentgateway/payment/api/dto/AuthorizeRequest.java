package com.ficmart.paymentgateway.payment.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AuthorizeRequest {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private Long amountInCents;

    @NotBlank(message = "Currency is required")
    @Pattern(
            regexp = "^[A-Za-z]{3}$",
            message = "Currency must be a 3-letter code"
    )
    private String currency;

    @NotBlank(message = "Card number is required")
    @Pattern(
            regexp = "^\\d{16}$",
            message = "Card number must contain exactly 16 digits"
    )
    private String cardNumber;

    @NotBlank(message = "CVV is required")
    @Pattern(
            regexp = "^\\d{3,4}$",
            message = "CVV must contain 3 or 4 digits"
    )
    private String cvv;

    @NotNull(message = "Expiry month is required")
    @Min(value = 1, message = "Expiry month must be between 1 and 12")
    @Max(value = 12, message = "Expiry month must be between 1 and 12")
    private Integer expiryMonth;

    @NotNull(message = "Expiry year is required")
    @Min(value = 2026, message = "Expiry year is invalid")
    private Integer expiryYear;
}