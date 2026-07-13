package com.ficmart.paymentgateway.payment.infrastructure.bank.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;

@Data
public class BankVoidResponse {
    @JsonProperty("authorization_id")
    private String authorizationId;
    private String status;

    @JsonProperty("void_id")
    private String voidId;

    @JsonProperty("voided_at")
    private Instant voidedAt;
}
