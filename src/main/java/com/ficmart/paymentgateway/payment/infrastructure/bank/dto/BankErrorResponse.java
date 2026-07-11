package com.ficmart.paymentgateway.payment.infrastructure.bank.dto;

import lombok.Data;

@Data
public class BankErrorResponse {
    private String error;
    private String message;
}
