package com.ficmart.paymentgateway.payment.infrastructure.bank.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;

@Data
public class BankCaptureResponse {
    private Long amount;

    @JsonProperty("authorization_id")
    private String authorizationId;

    @JsonProperty("capture_id")
    private String captureId;

    @JsonProperty("captured_at")
    private Instant capturedAt;
    private String currency;
    private String status;

}
