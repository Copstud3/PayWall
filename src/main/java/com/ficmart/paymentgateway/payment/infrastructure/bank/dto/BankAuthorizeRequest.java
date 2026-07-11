package com.ficmart.paymentgateway.payment.infrastructure.bank.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BankAuthorizeRequest {
        private Long amount;

        @JsonProperty("card_number")
        private String cardNumber;

        private String cvv;

        @JsonProperty("expiry_month")
        private Integer expiryMonth;

        @JsonProperty("expiry_year")
        private Integer expiryYear;

}
