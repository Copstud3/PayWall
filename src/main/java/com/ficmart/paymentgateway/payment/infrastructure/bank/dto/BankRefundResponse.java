package com.ficmart.paymentgateway.payment.infrastructure.bank.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;

@Data
public class BankRefundResponse {

    private Long amount;

    @JsonProperty("capture_id")
    private String captureId;

    private String currency;

    @JsonProperty("refund_id")
    private String refundId;

    @JsonProperty("refunded_at")
    private Instant refundedAt;

    private String status;
}
