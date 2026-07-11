package com.ficmart.paymentgateway.payment.infrastructure.bank.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class MockBankConfig {

    @Bean
    public RestClient mockBankRestClient(
            @Value("${bank.base-url}") String baseUrl
    ) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}