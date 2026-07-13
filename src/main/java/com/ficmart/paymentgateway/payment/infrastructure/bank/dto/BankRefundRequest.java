package com.ficmart.paymentgateway.payment.infrastructure.bank.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BankRefundRequest {

    private Long amount;

    @JsonProperty("capture_id")
    private String captureId;
}
