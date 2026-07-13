package com.ficmart.paymentgateway.payment.infrastructure.bank.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BankVoidRequest {

    @JsonProperty("authorization_id")
    private String authorizationId;
}
