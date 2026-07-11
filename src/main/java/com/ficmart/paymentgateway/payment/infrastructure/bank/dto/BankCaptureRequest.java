package com.ficmart.paymentgateway.payment.infrastructure.bank.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BankCaptureRequest {
    private Long amount;

    @JsonProperty("authorization_id")
    private String authorizationId;
}
