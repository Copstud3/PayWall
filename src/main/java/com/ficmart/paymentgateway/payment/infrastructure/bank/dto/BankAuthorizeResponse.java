package com.ficmart.paymentgateway.payment.infrastructure.bank.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;

@Data
public class BankAuthorizeResponse {
    private Long amount;

    @JsonProperty("authorization_id")
    private String authorizationId;

    @JsonProperty("created_at")
    private Instant createdAt;

    private String currency;

    @JsonProperty("expires_at")
    private Instant expiresAt;

    private String status;
}
